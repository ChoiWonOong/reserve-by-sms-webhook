package wonoong.sms_webhook.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wonoong.sms_webhook.client.CamfitClient;
import wonoong.sms_webhook.dto.camfit.CamfitLoginRequestDto;

import java.util.Map;

@RestController
@RequestMapping("/camfit")
@RequiredArgsConstructor
public class CamfitController {
    private final CamfitClient camfitClient;

    /**
     * Camfit 로그인 테스트
     * POST /camfit/login
     */
    @PostMapping("/login")
    public String testLogin(@RequestBody CamfitLoginRequestDto dto) {
        camfitClient.login(dto.identifier(), dto.password());
        return "테스트 요청 완료. 서버 로그를 확인하세요.";
    }

    /**
     * 캠핏 예약 차단 테스트
     */
    @PostMapping("/block")
    public Map<String, String> blockSchedule(
            @RequestParam String siteId,
            @RequestParam long startTimestamp,
            @RequestParam long endTimestamp,
            @RequestParam(defaultValue = "테스트") String title
    ) {
        String scheduleId = camfitClient.blockSchedule(
                siteId,
                startTimestamp,
                endTimestamp,
                title
        );

        return Map.of(
                "message", "캠핏 예약 차단 성공",
                "scheduleId", scheduleId,
                "siteId", siteId
        );
    }

    /**
     * 캠핏 예약 차단 해제 테스트
     */
    @DeleteMapping("/release")
    public Map<String, String> releaseSchedule(
            @RequestParam String scheduleId,
            @RequestParam String siteId
    ) {
        camfitClient.releaseSchedule(scheduleId, siteId);

        return Map.of(
                "message", "캠핏 예약 차단 해제 성공",
                "scheduleId", scheduleId,
                "siteId", siteId
        );
    }
    @PostMapping("/refresh")
    public ResponseEntity<String> refreshToken() {
        camfitClient.refreshToken();

        return ResponseEntity.ok(
                "Camfit 토큰이 성공적으로 갱신되었습니다."
        );
    }
}
