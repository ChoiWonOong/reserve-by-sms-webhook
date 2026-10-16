package wonoong.sms_webhook.dto.tripEleven;

import java.time.LocalDate;

public record TripElevenRoomStateRequestDto(
        String accommodationId,
        String bid,
        String roomId,
        LocalDate date
) {
}