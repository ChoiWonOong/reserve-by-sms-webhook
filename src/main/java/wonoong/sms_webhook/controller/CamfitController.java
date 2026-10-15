package wonoong.sms_webhook.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wonoong.sms_webhook.client.CamfitClient;
import wonoong.sms_webhook.dto.camfit.CamfitLoginRequestDto;

@RestController
@RequestMapping("/camfit")
@RequiredArgsConstructor
public class CamfitController {
    private final CamfitClient camfitClient;

    /**
     * Camfit 로그인 테스트
     * POST /test/camfit/login
     */
    @PostMapping("/login")
    public String testLogin(@RequestBody CamfitLoginRequestDto dto) {
        camfitClient.login(dto.identifier(), dto.password());
        return "테스트 요청 완료. 서버 로그를 확인하세요.";
    }
}
