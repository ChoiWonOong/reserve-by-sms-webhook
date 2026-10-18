package wonoong.sms_webhook.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wonoong.sms_webhook.dto.ReceivedMessageDto;
import wonoong.sms_webhook.entity.Reservation;
import wonoong.sms_webhook.entity.ReservationEvent;
import wonoong.sms_webhook.service.MessageParsingService;
import wonoong.sms_webhook.service.ReservationService;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageParsingService messageParsingService;
    private final ReservationService reservationService;

    /**
     * 공통 문자 수신
     * POST /api/messages
     */
    @PostMapping
    public ResponseEntity<Reservation> receiveMessage(
            @RequestBody ReceivedMessageDto request
    ) {
        return processMessage(request);
    }

    /**
     * TripEleven 문자 수신
     * POST /api/messages/trip-eleven
     */
    @PostMapping("/trip-eleven")
    public ResponseEntity<Reservation> receiveTripElevenMessage(
            @RequestBody ReceivedMessageDto request
    ) {
        return processMessage(request);
    }

    /**
     * Camfit 문자 수신
     * POST /api/messages/camfit
     */
    @PostMapping("/camfit")
    public ResponseEntity<Reservation> receiveCamfitMessage(
            @RequestBody ReceivedMessageDto request
    ) {
        return processMessage(request);
    }

    /**
     * 문자 파싱 → ReservationEvent 생성 → DB 확인 및 차단 처리
     */
    private ResponseEntity<Reservation> processMessage(
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
                messageParsingService.parse(request.body());

        // 2. 예약 DB 확인 후 반대 플랫폼 차단 또는 해제
        Reservation reservation =
                reservationService.processEvent(event);

        // 3. 처리 결과 반환
        return ResponseEntity.ok(reservation);
    }

    /**
     * 파싱만 테스트
     * POST /api/messages/test
     */
    @PostMapping("/test")
    public ResponseEntity<ReservationEvent> parseMessage(
            @RequestBody ParseMessageRequest request
    ) {
        ReservationEvent result =
                messageParsingService.parse(request.message());

        return ResponseEntity.ok(result);
    }

    public record ParseMessageRequest(String message) {
    }
}