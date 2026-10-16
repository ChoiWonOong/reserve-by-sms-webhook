package wonoong.sms_webhook.parser;

import org.springframework.stereotype.Component;
import wonoong.sms_webhook.entity.ReservationEvent;

@Component
public class CamfitMessageParser implements MessageParser {

    @Override
    public boolean supports(String message) {
        return message != null
                && message.contains("하이몽글램핑&캠핑장")
                && message.contains("캠핑존 :");
    }

    @Override
    public ReservationEvent parse(String message) {
        // 기존에 작성한 캠핏 파싱 로직
        // 예약번호, 사이트명, 입실일, 퇴실일 추출
        throw new UnsupportedOperationException("기존 파싱 로직으로 교체");
    }
}