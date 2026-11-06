package wonoong.sms_webhook.dto.tripEleven;

import java.time.LocalDate;

public record TripElevenReservationDetailDto(
        LocalDate checkInDate,
        LocalDate checkOutDate,
        int nights
) {
}