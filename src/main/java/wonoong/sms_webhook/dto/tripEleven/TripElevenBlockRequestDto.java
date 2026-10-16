package wonoong.sms_webhook.dto.tripEleven;

import java.time.LocalDate;

public record TripElevenBlockRequestDto(
        String accommodationId,
        String bid,
        String roomId,
        LocalDate date,
        String reason,
        String reasonText
) {
}