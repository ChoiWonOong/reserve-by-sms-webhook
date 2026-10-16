package wonoong.sms_webhook.entity;

import java.time.LocalDate;

public record ReservationEvent(
        Platform platform,
        ReservationEventType eventType,
        String reservationId,
        String externalSiteName,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        int nights
) {
}