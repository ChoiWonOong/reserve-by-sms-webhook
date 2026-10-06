package wonoong.sms_webhook.dto;

public record ReceivedMessageDto(
        String type,
        String sender,
        String body,
        long receivedAt
) {
}