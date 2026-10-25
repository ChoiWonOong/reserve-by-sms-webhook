
package wonoong.sms_webhook.parser;

import org.springframework.stereotype.Component;
import wonoong.sms_webhook.entity.Platform;
import wonoong.sms_webhook.entity.ReservationEvent;
import wonoong.sms_webhook.entity.ReservationEventType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TripElevenCampingMessageParser implements MessageParser {

    private static final String RESERVATION_TITLE =
            "[가평 하이몽글램핑&캠핑-예약완료]";

    private static final String CANCELLATION_TITLE =
            "[가평 하이몽글램핑&캠핑-취소접수완료]";

    // 예약 문자
    private static final Pattern RESERVATION_INFO_PATTERN =
            Pattern.compile(
                    "(?m)^\\s*(\\d+)\\s*/\\s*(.+?)(?:\\s*\\((\\d{9,11})\\))?\\s*$"
            );

    private static final Pattern SITE_NAME_PATTERN =
            Pattern.compile(
                    "(?m)^\\s*객실명\\s*:\\s*(.+?)\\s*$"
            );

    private static final Pattern CHECK_IN_PATTERN =
            Pattern.compile(
                    "입실일\\s*:\\s*" +
                            "(\\d{4}-\\d{2}-\\d{2})" +
                            "\\s*\\([^)]*\\)\\s*/\\s*(\\d+)박"
            );

    @Override
    public boolean supports(String message) {
        if (message == null) {
            return false;
        }

        boolean isReservation =
                message.contains(RESERVATION_TITLE);

        boolean isCancellation =
                message.contains(CANCELLATION_TITLE);

        return (isReservation || isCancellation)
                && message.contains("객실명")
                && message.contains("입실일");
    }

    @Override
    public ReservationEvent parse(String message) {
        if (!supports(message)) {
            throw new IllegalArgumentException(
                    "트립일레븐 예약 또는 취소 문자 형식이 아닙니다."
            );
        }

        ReservationEventType eventType =
                message.contains(CANCELLATION_TITLE)
                        ? ReservationEventType.CANCELLED
                        : ReservationEventType.RESERVED;

        Matcher reservationMatcher =
                RESERVATION_INFO_PATTERN.matcher(message);

        if (!reservationMatcher.find()) {
            throw new IllegalArgumentException(
                    "예약번호 또는 예약자 정보를 추출할 수 없습니다."
            );
        }

        String reservationId = reservationMatcher.group(1);
        String customerName = reservationMatcher.group(2).trim();
        String customerPhone = reservationMatcher.group(3);

        String siteName = extract(
                SITE_NAME_PATTERN,
                message,
                "객실명"
        ).trim();

        Matcher dateMatcher = CHECK_IN_PATTERN.matcher(message);

        if (!dateMatcher.find()) {
            throw new IllegalArgumentException(
                    "입실일 또는 숙박 일수를 추출할 수 없습니다."
            );
        }

        LocalDate checkInDate = LocalDate.parse(
                dateMatcher.group(1),
                DateTimeFormatter.ISO_LOCAL_DATE
        );

        int nights = Integer.parseInt(dateMatcher.group(2));

        if (nights <= 0) {
            throw new IllegalArgumentException(
                    "숙박 일수는 1 이상이어야 합니다."
            );
        }

        LocalDate checkOutDate = checkInDate.plusDays(nights);

        return new ReservationEvent(
                Platform.TRIP_ELEVEN,
                eventType,
                reservationId,
                siteName,
                checkInDate,
                checkOutDate,
                nights,
                customerName,
                customerPhone
        );
    }

    private String extract(
            Pattern pattern,
            String message,
            String fieldName
    ) {
        Matcher matcher = pattern.matcher(message);

        if (!matcher.find()) {
            throw new IllegalArgumentException(
                    fieldName + "을(를) 추출할 수 없습니다."
            );
        }

        return matcher.group(1);
    }
}
