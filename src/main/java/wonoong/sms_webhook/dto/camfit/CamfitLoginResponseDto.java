package wonoong.sms_webhook.dto.camfit;

public record CamfitLoginResponseDto(
        String jwt,
        String refreshToken,
        boolean isHibernating,
        CamfitUser user,
        boolean isCertified
) {
    public record CamfitUser(
            String id,
            String username,
            String email,
            CamfitRole role,
            boolean isHostApproved,
            boolean isHostRejected
    ) {}

    public record CamfitRole(String name) {}
}