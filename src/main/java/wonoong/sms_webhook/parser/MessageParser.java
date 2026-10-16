package wonoong.sms_webhook.parser;

import wonoong.sms_webhook.entity.ReservationEvent;

public interface MessageParser {

    ReservationEvent parse(String message);

    boolean supports(String message);
}