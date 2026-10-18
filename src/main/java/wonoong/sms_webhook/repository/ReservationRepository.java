
package wonoong.sms_webhook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wonoong.sms_webhook.entity.Platform;
import wonoong.sms_webhook.entity.Reservation;

import java.util.Optional;

public interface ReservationRepository
        extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByPlatformAndReservationId(
            Platform platform,
            String reservationId
    );
}
