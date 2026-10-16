package wonoong.sms_webhook.parser;

import org.springframework.stereotype.Component;
import wonoong.sms_webhook.entity.Platform;
import wonoong.sms_webhook.entity.ReservationEvent;
import wonoong.sms_webhook.entity.ReservationEventType;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CamfitMessageParser implements MessageParser {

    private static final String CAMFIT_KEYWORD =
            "하이몽글램핑&캠핑장";

    // 예약번호 : C20261002003806
    private static final Pattern RESERVATION_ID_PATTERN =
            Pattern.compile("예약번호\\s*:\\s*(\\S+)");

    // 캠핑존 : C동 (타프-데크) / C11타프데크
    private static final Pattern SITE_NAME_PATTERN =
            Pattern.compile(
                    "캠핑존\\s*:\\s*[^\\r\\n]*?/\\s*(.+)"
            );

    // 고객정보 : 김다슬 / 01073119923
    private static final Pattern CUSTOMER_INFO_PATTERN =
            Pattern.compile(
                    "고객정보\\s*:\\s*(.+?)\\s*/\\s*(\\d{9,11})"
            );

    // 입실일자 : 2026/10/31 ~ 2026/11/1 (1박)
    private static final Pattern DATE_PATTERN =
            Pattern.compile(
                    "입실일자\\s*:\\s*" +
                            "(\\d{4})/(\\d{1,2})/(\\d{1,2})\\s*~\\s*" +
                            "(\\d{4})/(\\d{1,2})/(\\d{1,2})\\s*" +
                            "\\((\\d+)박\\)"
            );

    @Override
    public boolean supports(String message) {
        return message != null
                && message.contains(CAMFIT_KEYWORD)
                && message.contains("캠핑존")
                && message.contains("예약번호");
    }

    @Override
    public ReservationEvent parse(String message) {
        if (!supports(message)) {
            throw new IllegalArgumentException(
                    "캠핏 예약 문자 형식이 아닙니다."
            );
        }

        String reservationId = extract(
                RESERVATION_ID_PATTERN,
                message,
                "예약번호"
        );

        String siteName = extract(
                SITE_NAME_PATTERN,
                message,
                "캠핑존"
        ).trim();

        Matcher customerMatcher =
                CUSTOMER_INFO_PATTERN.matcher(message);

        if (!customerMatcher.find()) {
            throw new IllegalArgumentException(
                    "캠핏 문자에서 고객 이름 또는 전화번호를 추출할 수 없습니다."
            );
        }

        String customerName = customerMatcher.group(1).trim();
        String customerPhone = customerMatcher.group(2);

        Matcher dateMatcher = DATE_PATTERN.matcher(message);

        if (!dateMatcher.find()) {
            throw new IllegalArgumentException(
                    "캠핏 문자에서 입실일자 또는 숙박 일수를 추출할 수 없습니다."
            );
        }

        LocalDate checkInDate = LocalDate.of(
                Integer.parseInt(dateMatcher.group(1)),
                Integer.parseInt(dateMatcher.group(2)),
                Integer.parseInt(dateMatcher.group(3))
        );

        LocalDate checkOutDate = LocalDate.of(
                Integer.parseInt(dateMatcher.group(4)),
                Integer.parseInt(dateMatcher.group(5)),
                Integer.parseInt(dateMatcher.group(6))
        );

        int nights = Integer.parseInt(dateMatcher.group(7));

        if (nights <= 0 || !checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException(
                    "입실일, 퇴실일 또는 숙박 일수가 올바르지 않습니다."
            );
        }

        ReservationEventType eventType;

        if (message.contains("신규 예약이 성사되었습니다.")) {
            eventType = ReservationEventType.RESERVED;
        } else if (message.contains("예약이 취소되었습니다.")) {
            eventType = ReservationEventType.CANCELLED;
        } else {
            throw new IllegalArgumentException(
                    "지원하지 않는 캠핏 예약 이벤트입니다."
            );
        }

        return new ReservationEvent(
                Platform.CAMFIT,
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
                    "캠핏 문자에서 " + fieldName
                            + "을(를) 추출할 수 없습니다."
            );
        }

        return matcher.group(1);
    }
}