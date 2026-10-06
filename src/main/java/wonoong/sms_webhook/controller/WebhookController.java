package wonoong.sms_webhook.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wonoong.sms_webhook.dto.WebhookDto;
import wonoong.sms_webhook.service.WebhookService;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService smsWebhookService;

    @PostMapping("/message")
    public ResponseEntity<Void> receiveSms(
            @RequestBody WebhookDto request
    ) {

        smsWebhookService.process(request);

        return ResponseEntity.ok().build();
    }
}