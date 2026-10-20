package wonoong.sms_webhook.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wonoong.sms_webhook.dto.ReceivedMessageDto;
import wonoong.sms_webhook.entity.Reservation;
import wonoong.sms_webhook.entity.ReservationEvent;
import wonoong.sms_webhook.service.MessageService;
import wonoong.sms_webhook.service.ReservationService;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final ReservationService reservationService;

    /**
     * 공통 문자 수신
     * POST /api/messages
     */
    @PostMapping
    public ResponseEntity<Reservation> receiveMessage(
            @RequestBody ReceivedMessageDto request
    ) {
        return messageService.processMessage(request);
    }

    /**
     * TripEleven 문자 수신
     * POST /api/messages/trip-eleven
     */
    @PostMapping("/trip-eleven")
    public ResponseEntity<Reservation> receiveTripElevenMessage(
            @RequestBody ReceivedMessageDto request
    ) {
        return messageService.processMessage(request);
    }

    /**
     * Camfit 문자 수신
     * POST /api/messages/camfit
     */
    @PostMapping("/camfit")
    public ResponseEntity<Reservation> receiveCamfitMessage(
            @RequestBody ReceivedMessageDto request
    ) {
        return messageService.processMessage(request);
    }
}