
package wonoong.sms_webhook.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(
        name = "reservation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reservation_platform_id",
                columnNames = {"platform", "reservation_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Platform platform;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReservationEventType eventType;

    @Column(name = "reservation_id", nullable = false)
    private String reservationId;

    @Column(nullable = false)
    private LocalDate checkInDate;

    @Column(nullable = false)
    private LocalDate checkOutDate;

    @Column(nullable = false)
    private int nights;

    private String customerName;

    private String customerPhone;

    // PENDING, SUCCESS, FAILED
    @Column(nullable = false)
    private String syncStatus = "PENDING";

    // 예약 API 필요 정보
    private String scheduleId;
    private String externalSiteId;

    public static Reservation from(ReservationEvent event) {
        Reservation reservation = new Reservation();

        reservation.platform = event.platform();
        reservation.eventType = event.eventType();
        reservation.reservationId = event.reservationId();
        reservation.checkInDate = event.checkInDate();
        reservation.checkOutDate = event.checkOutDate();
        reservation.nights = event.nights();
        reservation.customerName = event.customerName();
        reservation.customerPhone = event.customerPhone();

        return reservation;
    }
}
