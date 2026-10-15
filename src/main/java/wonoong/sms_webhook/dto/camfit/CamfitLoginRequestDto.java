package wonoong.sms_webhook.dto.camfit;

public record CamfitLoginRequestDto(
        String identifier,
        String password
) {
}