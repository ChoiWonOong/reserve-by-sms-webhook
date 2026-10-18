package wonoong.sms_webhook.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wonoong.sms_webhook.entity.Reservation;
import wonoong.sms_webhook.entity.ReservationEvent;
import wonoong.sms_webhook.service.ReservationService;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping("/event")
    public ResponseEntity<Reservation> processEvent(
            @RequestBody ReservationEvent event
    ) {
        return ResponseEntity.ok(
                reservationService.processEvent(event)
        );
    }
}