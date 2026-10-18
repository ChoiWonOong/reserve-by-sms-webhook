package wonoong.sms_webhook.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import wonoong.sms_webhook.Config.CamfitProperties;
import wonoong.sms_webhook.dto.camfit.CamfitLoginRequestDto;
import wonoong.sms_webhook.dto.camfit.CamfitLoginResponseDto;

import java.util.Map;
@Slf4j
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
    private String getAccessToken() {
        String accessToken = redisTemplate.opsForValue()
                .get(ACCESS_TOKEN_KEY);

        // 1. Access Token이 있으면 그대로 사용
        if (accessToken != null && !accessToken.isBlank()) {
            return accessToken;
        }

        String refreshToken = redisTemplate.opsForValue()
                .get(REFRESH_TOKEN_KEY);

        // 2. Refresh Token이 있으면 토큰 갱신
        if (refreshToken != null && !refreshToken.isBlank()) {
            log.info("Camfit Refresh Token으로 Access Token을 갱신합니다.");
            refreshToken();
        } else {
            // 3. Refresh Token이 없으면 로그인
            log.info("Camfit Refresh Token이 없습니다. 로그인을 시도합니다.");
            login(camfitProperties.getUsername(), camfitProperties.getPassword());
        }

        // 4. 갱신 또는 로그인 후 Access Token 재조회
        accessToken = redisTemplate.opsForValue()
                .get(ACCESS_TOKEN_KEY);

        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException(
                    "Camfit 토큰 갱신 또는 로그인 후에도 Access Token이 없습니다."
            );
        }

        return accessToken;
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
                        identifier, password
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
        Map<String, Object> request = Map.of(
                "siteId", siteId,
                "startTimestamp", startTimestamp,
                "endTimestamp", endTimestamp,
                "title", title
        );

        // 최초 요청
        String accessToken = getAccessToken();

        try {
            return executeBlockSchedule(
                    request,
                    accessToken
            );
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("Camfit Access Token 만료. 토큰 갱신 후 재시도합니다.");

            refreshToken();

            // 갱신된 토큰으로 한 번만 재시도
            String refreshedAccessToken = getAccessToken();

            return executeBlockSchedule(
                    request,
                    refreshedAccessToken
            );
        }
    }
    private String executeBlockSchedule(
            Map<String, Object> request,
            String accessToken
    ) {
        Map<?, ?> response = restClient.post()
                .uri("/schedules")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException("Camfit 응답이 없습니다.");
        }

        Object scheduleId = response.get("scheduleId");

        if (scheduleId == null
                || scheduleId.toString().isBlank()) {
            throw new IllegalStateException(
                    "Camfit 예약 차단 응답에 scheduleId가 없습니다: "
                            + response
            );
        }

        log.info("Camfit 차단 성공: scheduleId={}", scheduleId);

        return scheduleId.toString();
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