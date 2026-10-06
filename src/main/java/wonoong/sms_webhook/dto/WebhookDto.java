package wonoong.sms_webhook.dto;

public record WebhookDto(
        String deviceId,
        String event,
        String id,
        Payload payload,
        String webhookId
) {

    public record Payload(
            String messageId,
            String message,
            String sender,
            String recipient,
            Integer simNumber,
            String receivedAt
    ) {
    }
}
