package wonoong.sms_webhook.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wonoong.sms_webhook.dto.ReceivedMessageDto;
import wonoong.sms_webhook.entity.ReservationEvent;
import wonoong.sms_webhook.service.MessageParsingService;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageParsingService messageParsingService;

    @PostMapping
    public ResponseEntity<Void> receiveMessage(
            @RequestBody ReceivedMessageDto request
    ) {

        System.out.println("================================");
        System.out.println("메시지 수신");
        System.out.println("type       = " + request.type());
        System.out.println("sender     = " + request.sender());
        System.out.println("body       = " + request.body());
        System.out.println("receivedAt = " + request.receivedAt());
        System.out.println("================================");

        return ResponseEntity.ok().build();
    }
    @PostMapping("/trip-eleven")
    public ResponseEntity<Void> receiveTripElevenMessage(
            @RequestBody ReceivedMessageDto request
    ) {

        System.out.println("================================");
        System.out.println("Trip Eleven 메시지 수신");
        System.out.println("type       = " + request.type());
        System.out.println("sender     = " + request.sender());
        System.out.println("body       = " + request.body());
        System.out.println("receivedAt = " + request.receivedAt());
        System.out.println("================================");

        return ResponseEntity.ok().build();
    }
    @PostMapping("/camfit")
    public ResponseEntity<Void> receiveCamfitMessage(
            @RequestBody ReceivedMessageDto request
    ) {

        System.out.println("================================");
        System.out.println("Camfit 메시지 수신");
        System.out.println("type       = " + request.type());
        System.out.println("sender     = " + request.sender());
        System.out.println("body       = " + request.body());
        System.out.println("receivedAt = " + request.receivedAt());
        System.out.println("================================");

        return ResponseEntity.ok().build();
    }

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