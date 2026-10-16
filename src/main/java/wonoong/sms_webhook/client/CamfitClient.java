package wonoong.sms_webhook.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import wonoong.sms_webhook.Config.CamfitProperties;
import wonoong.sms_webhook.dto.camfit.CamfitLoginRequestDto;
import wonoong.sms_webhook.dto.camfit.CamfitLoginResponseDto;

import java.util.Map;

@Component
public class CamfitClient {
    private final CamfitProperties camfitProperties;
    private final RestClient restClient;
    private final StringRedisTemplate redisTemplate;

    private static final String ACCESS_TOKEN_KEY =
            "camfit:access-token";

    private static final String REFRESH_TOKEN_KEY =
            "camfit:refresh-token";

    public CamfitClient(
            CamfitProperties camfitProperties,
            @Qualifier("camfitRestClient") RestClient restClient,
            StringRedisTemplate redisTemplate) {
        this.restClient = restClient;
        this.camfitProperties = camfitProperties;
        this.redisTemplate = redisTemplate;
    }

    public void login(
            String identifier,
            String password
    ) {
        CamfitLoginResponseDto response = restClient.post()
                .uri("/auth/local")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(new CamfitLoginRequestDto(
                        camfitProperties.getUsername(),
                        camfitProperties.getPassword()
                ))
                .retrieve()
                .body(CamfitLoginResponseDto.class);
        if (response == null) {
            throw new IllegalStateException("캠핏 로그인 응답이 비어 있습니다.");
        }
        saveTokens(response.jwt(), response.refreshToken());
    }
    public void refreshToken() {
        String refreshToken = redisTemplate.opsForValue()
                .get(REFRESH_TOKEN_KEY);
        System.out.println("Redis key: " + REFRESH_TOKEN_KEY);
        System.out.println("Refresh token exists: "
                + (refreshToken != null && !refreshToken.isBlank()));
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalStateException(
                    "Redis에 Camfit Refresh Token이 없습니다."
            );
        }

        JsonNode response = restClient.post()
                .uri("/auth/refresh")
                .header("Refresh-Token", refreshToken)
                .retrieve()
                .body(JsonNode.class);

        if (response == null
                || !response.hasNonNull("jwt")
                || !response.hasNonNull("refreshToken")) {
            throw new IllegalStateException(
                    "Camfit 토큰 갱신 응답이 올바르지 않습니다."
            );
        }

        redisTemplate.opsForValue().set(
                ACCESS_TOKEN_KEY,
                response.get("jwt").asText()
        );

        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_KEY,
                response.get("refreshToken").asText()
        );
    }

    public void saveTokens(
            String accessToken,
            String refreshToken
    ) {
        redisTemplate.opsForValue()
                .set(ACCESS_TOKEN_KEY, accessToken);

        redisTemplate.opsForValue()
                .set(REFRESH_TOKEN_KEY, refreshToken);
    }

    public String blockSchedule(
            String siteId,
            long startTimestamp,
            long endTimestamp,
            String title
    ) {
        String accessToken = redisTemplate.opsForValue()
                .get(ACCESS_TOKEN_KEY);

        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException(
                    "캠핏 Access Token이 없습니다. 로그인이 필요합니다."
            );
        }

        Map<String, Object> request = Map.of(
                "siteId", siteId,
                "startTimestamp", startTimestamp,
                "endTimestamp", endTimestamp,
                "title", title
        );

        Map<?, ?> response = restClient.post()
                .uri("/schedules")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("scheduleId") == null) {
            throw new IllegalStateException(
                    "캠핏 예약 차단 응답에 scheduleId가 없습니다."
            );
        }

        return response.get("scheduleId").toString();
    }

    public String releaseSchedule(String scheduleId, String siteId) {
        String accessToken = redisTemplate.opsForValue()
                .get(ACCESS_TOKEN_KEY);

        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException(
                    "캠핏 Access Token이 없습니다. 로그인이 필요합니다."
            );
        }

        return restClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/schedules/{scheduleId}")
                        .queryParam("siteId", siteId)
                        .build(scheduleId))
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .toBodilessEntity().toString();
    }
}