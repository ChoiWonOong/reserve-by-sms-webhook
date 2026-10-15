package wonoong.sms_webhook.dto.camfit;

public record CamfitTokenResponseDto(
        String accessToken,
        String refreshToken
) {}
