package wonoong.sms_webhook.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import wonoong.sms_webhook.dto.ReceivedMessageDto;
import wonoong.sms_webhook.entity.Reservation;
import wonoong.sms_webhook.entity.ReservationEvent;
import wonoong.sms_webhook.parser.MessageParser;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final List<MessageParser> parsers;
    private final ReservationService reservationService;
    /**
     * 문자 파싱 → ReservationEvent 생성 → DB 확인 및 차단 처리
     */
    public ResponseEntity<Reservation> processMessage(
            ReceivedMessageDto request
    ) {
        System.out.println("================================");
        System.out.println("문자 수신");
        System.out.println("type       = " + request.type());
        System.out.println("sender     = " + request.sender());
        System.out.println("body       = " + request.body());
        System.out.println("receivedAt = " + request.receivedAt());
        System.out.println("================================");

        // 1. 문자 원문 파싱
        ReservationEvent event =
                parse(request.body());

        // 2. 예약 DB 확인 후 반대 플랫폼 차단 또는 해제
        Reservation reservation =
                reservationService.processEvent(event);

        // 3. 처리 결과 반환
        return ResponseEntity.ok(reservation);
    }

    public ReservationEvent parse(String message) {
        List<MessageParser> matched = parsers.stream()
                .filter(parser -> parser.supports(message))
                .toList();

        if (matched.isEmpty()) {
            return null;
        }

        if (matched.size() > 1) {
            throw new IllegalStateException(
                    "여러 파서가 동일한 문자를 지원합니다."
            );
        }

        return matched.getFirst().parse(message);
    }
}