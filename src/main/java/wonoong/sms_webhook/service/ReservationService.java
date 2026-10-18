package wonoong.sms_webhook.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import wonoong.sms_webhook.client.CamfitClient;
import wonoong.sms_webhook.client.TripElevenClient;
import wonoong.sms_webhook.entity.ExternalSite;
import wonoong.sms_webhook.entity.Platform;
import wonoong.sms_webhook.entity.Reservation;
import wonoong.sms_webhook.entity.ReservationEvent;
import wonoong.sms_webhook.entity.ReservationEventType;
import wonoong.sms_webhook.repository.ExternalSiteRepository;
import wonoong.sms_webhook.repository.ReservationRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private static final String TRIP_ELEVEN_BID = "6996";
    private static final String PENDING = "PENDING";
    private static final String SUCCESS = "SUCCESS";
    private static final String FAILED = "FAILED";

    private final TripElevenClient tripElevenClient;
    private final CamfitClient camfitClient;
    private final ExternalSiteRepository externalSiteRepository;
    private final ReservationRepository reservationRepository;

    /**
     * 예약 이벤트 처리 진입점
     */
    public Reservation processEvent(ReservationEvent event) {
        if (event.eventType() == ReservationEventType.CANCELLED) {
            return cancelReservation(event);
        }

        Reservation reservation = reservationRepository
                .findByPlatformAndReservationId(
                        event.platform(),
                        event.reservationId()
                )
                .orElseGet(() ->
                        reservationRepository.saveAndFlush(
                                Reservation.from(event)
                        )
                );

        // 이미 동기화에 성공한 예약은 중복 처리하지 않는다.
        if (SUCCESS.equals(reservation.getSyncStatus())) {
            return reservation;
        }

        try {
            reservation.setSyncStatus(PENDING);
            reservationRepository.saveAndFlush(reservation);

            if (event.platform() == Platform.CAMFIT) {
                blockTripEleven(event, reservation);
            } else if (event.platform() == Platform.TRIP_ELEVEN) {
                blockCamfit(event, reservation);
            } else {
                throw new IllegalArgumentException(
                        "지원하지 않는 플랫폼입니다: " + event.platform()
                );
            }

            reservation.setEventType(event.eventType());
            reservation.setSyncStatus(SUCCESS);

        } catch (Exception e) {
            reservation.setSyncStatus(FAILED);
            reservationRepository.saveAndFlush(reservation);

            throw new IllegalStateException(
                    "예약 동기화에 실패했습니다. reservationId="
                            + event.reservationId(),
                    e
            );
        }

        return reservationRepository.saveAndFlush(reservation);
    }

    /**
     * TripEleven 예약을 기준으로 Camfit 사이트 차단
     */
    private void blockCamfit(
            ReservationEvent event,
            Reservation reservation
    ) {
        ExternalSite target = findLinkedExternalSite(
                Platform.TRIP_ELEVEN,
                event.externalSiteName(),
                Platform.CAMFIT
        );

        ZoneId zoneId = ZoneId.of("Asia/Seoul");

        long startTimestamp = event.checkInDate()
                .atTime(9,59)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli();

        long endTimestamp = event.checkOutDate()
                .atTime(16, 0)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli();

        String title = String.format(
                "트립 %s %s",
                event.customerName(),
                event.customerPhone()
        );
        String siteId = target.getExternalSiteId();


        log.info("Camfit 차단 요청: siteId={}", target.getExternalSiteId());

        String scheduleId = camfitClient.blockSchedule(
                target.getExternalSiteId(),
                startTimestamp,
                endTimestamp,
                title
        );

        log.info("Camfit 차단 응답: {}", scheduleId);

        if (scheduleId == null || scheduleId.isBlank()) {
            throw new IllegalStateException(
                    "Camfit 차단 API에서 scheduleId를 반환하지 않았습니다: siteId="
                            + target.getExternalSiteId()
            );
        }

// 취소 시 사용할 차단 정보를 저장한다.
        reservation.setScheduleId(scheduleId);
        reservation.setExternalSiteId(target.getExternalSiteId());
    }

    /**
     * Camfit 예약을 기준으로 TripEleven 사이트 차단
     */
    private void blockTripEleven(
            ReservationEvent event,
            Reservation reservation
    ) {
        ExternalSite target = findLinkedExternalSite(
                Platform.CAMFIT,
                event.externalSiteName(),
                Platform.TRIP_ELEVEN
        );

        String externalSiteId = target.getExternalSiteId();

        String reasonText = String.format(
                "캠핏 %s %s",
                event.customerName(),
                event.customerPhone()
        );

        for (LocalDate date = event.checkInDate();
             date.isBefore(event.checkOutDate());
             date = date.plusDays(1)) {

            tripElevenClient.blockRoomDate(
                    TRIP_ELEVEN_BID,
                    externalSiteId,
                    date,
                    "그외펜션사유",
                    reasonText
            );
        }

        // 취소 시 사용할 차단 대상 사이트 ID
        reservation.setExternalSiteId(externalSiteId);
        reservation.setScheduleId(null);
    }

    /**
     * 예약 취소 처리
     */
    private Reservation cancelReservation(ReservationEvent event) {
        Reservation reservation = reservationRepository
                .findByPlatformAndReservationId(
                        event.platform(),
                        event.reservationId()
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "취소할 기존 예약을 찾을 수 없습니다: "
                                + event.platform() + " / "
                                + event.reservationId()
                ));

        if (reservation.getEventType() == ReservationEventType.CANCELLED
                && SUCCESS.equals(reservation.getSyncStatus())) {
            return reservation;
        }

        try {
            String externalSiteId = reservation.getExternalSiteId();

            if (externalSiteId == null || externalSiteId.isBlank()) {
                throw new IllegalStateException(
                        "차단 대상 externalSiteId가 없습니다."
                );
            }

            if (reservation.getPlatform() == Platform.TRIP_ELEVEN) {
                // TripEleven 예약으로 생성한 Camfit 차단 해제
                if (reservation.getScheduleId() == null
                        || reservation.getScheduleId().isBlank()) {
                    throw new IllegalStateException(
                            "Camfit scheduleId가 없습니다."
                    );
                }

                camfitClient.releaseSchedule(
                        reservation.getScheduleId(),
                        externalSiteId
                );

            } else if (reservation.getPlatform() == Platform.CAMFIT) {
                // Camfit 예약으로 생성한 TripEleven 차단 해제
                for (LocalDate date = reservation.getCheckInDate();
                     date.isBefore(reservation.getCheckOutDate());
                     date = date.plusDays(1)) {

                    tripElevenClient.unblockRoomDate(
                            TRIP_ELEVEN_BID,
                            externalSiteId,
                            date
                    );
                }

            } else {
                throw new IllegalArgumentException(
                        "지원하지 않는 플랫폼입니다: "
                                + reservation.getPlatform()
                );
            }

            reservation.setEventType(ReservationEventType.CANCELLED);
            reservation.setSyncStatus(SUCCESS);

        } catch (Exception e) {
            reservation.setSyncStatus(FAILED);
            reservationRepository.saveAndFlush(reservation);

            throw new IllegalStateException(
                    "예약 취소 동기화에 실패했습니다. reservationId="
                            + event.reservationId(),
                    e
            );
        }

        return reservationRepository.saveAndFlush(reservation);
    }

    /**
     * 원본 플랫폼의 사이트를 찾은 뒤 같은 내부 Site에 연결된
     * 대상 플랫폼의 사이트를 조회한다.
     */
    private ExternalSite findLinkedExternalSite(
            Platform sourcePlatform,
            String sourceExternalSiteName,
            Platform targetPlatform
    ) {
        ExternalSite source = externalSiteRepository
                .findByPlatformAndExternalSiteNameAndActiveIsTrue(
                        sourcePlatform,
                        sourceExternalSiteName
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "원본 사이트 매핑을 찾을 수 없습니다: "
                                + sourcePlatform + " / "
                                + sourceExternalSiteName
                ));

        Long siteId = source.getSite().getId();

        return externalSiteRepository
                .findBySite_IdAndPlatformAndActiveTrue(
                        siteId,
                        targetPlatform
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "대상 사이트 매핑을 찾을 수 없습니다: "
                                + targetPlatform + " / siteId=" + siteId
                ));
    }
}