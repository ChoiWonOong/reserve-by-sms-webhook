package wonoong.sms_webhook.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import wonoong.sms_webhook.entity.ReservationEvent;
import wonoong.sms_webhook.parser.MessageParser;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageParsingService {

    private final List<MessageParser> parsers;

    public ReservationEvent parse(String message) {
        List<MessageParser> matched = parsers.stream()
                .filter(parser -> parser.supports(message))
                .toList();

        if (matched.isEmpty()) {
            throw new IllegalArgumentException(
                    "지원하지 않는 예약 문자입니다."
            );
        }

        if (matched.size() > 1) {
            throw new IllegalStateException(
                    "여러 파서가 동일한 문자를 지원합니다."
            );
        }

        return matched.get(0).parse(message);
    }
}