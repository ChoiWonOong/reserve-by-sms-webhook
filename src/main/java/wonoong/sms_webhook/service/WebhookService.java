package wonoong.sms_webhook.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import wonoong.sms_webhook.dto.WebhookDto;

@Slf4j
@Service
public class WebhookService {

    public void process(WebhookDto request) {

        if (!"sms:received".equals(request.event())) {
            log.warn("지원하지 않는 webhook event: {}", request.event());
            return;
        }

        WebhookDto.Payload payload = request.payload();

        log.info("========== SMS RECEIVED ==========");
        log.info("발신번호: {}", payload.sender());
        log.info("수신번호: {}", payload.recipient());
        log.info("내용: {}", payload.message());
        log.info("수신시간: {}", payload.receivedAt());
        log.info("==================================");

        // TODO
        // 여기서 예약문자인지 확인
        // 예약정보 파싱
        // DB 저장
        // 다른 예약사이트 차단
    }
}