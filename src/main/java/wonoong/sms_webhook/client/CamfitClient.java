package wonoong.sms_webhook.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import wonoong.sms_webhook.Config.CamfitProperties;
import wonoong.sms_webhook.dto.camfit.CamfitLoginRequestDto;
import wonoong.sms_webhook.dto.camfit.CamfitLoginResponseDto;
import wonoong.sms_webhook.dto.camfit.CamfitTokenResponseDto;

import java.util.Map;

@Component
public class CamfitClient {
    private final CamfitProperties camfitProperties;
    private final RestClient restClient;
    private final StringRedisTemplate redisTemplate;

    private static final String ACCESS_TOKEN_KEY =
            "camfit:auth:access-token";

    private static final String REFRESH_TOKEN_KEY =
            "camfit:auth:refresh-token";

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
                .header("User-Agent", "Application")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CamfitLoginRequestDto(identifier, password))
                .retrieve()
                .body(CamfitLoginResponseDto.class);
        if (response == null) {
            throw new IllegalStateException("캠핏 로그인 응답이 비어 있습니다.");
        }
        saveTokens(response.jwt(), response.refreshToken());
    }
    public CamfitTokenResponseDto refreshToken(String refreshToken) {
        CamfitTokenResponseDto response = restClient.post()
                .uri("/auth/refresh")
                .header("User-Agent", "Application")
                .header("Refresh-Token", refreshToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of())
                .retrieve()
                .body(CamfitTokenResponseDto.class);

        return response;
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
//    public void releaseSchedule(String scheduleId, String siteId) {
//        restClient.delete()
//                .uri(uriBuilder -> uriBuilder
//                        .path("/schedules/{scheduleId}")
//                        .queryParam("siteId", siteId)
//                        .build(scheduleId))
//                .header("User-Agent", "Application")
//                .header("Authorization", "Bearer " + accessToken)
//                .retrieve()
//                .toBodilessEntity();
//    }
}