
package wonoong.sms_webhook.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import wonoong.sms_webhook.client.TripElevenClient;
import wonoong.sms_webhook.dto.tripEleven.TripElevenReservationDetailDto;
import wonoong.sms_webhook.dto.tripEleven.TripElevenReservationDto;
import wonoong.sms_webhook.entity.Platform;
import wonoong.sms_webhook.entity.Reservation;
import wonoong.sms_webhook.entity.ReservationEventType;
import wonoong.sms_webhook.repository.ReservationRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripElevenService {

    private final TripElevenClient tripElevenClient;
    private final StringRedisTemplate redisTemplate;
    private final ReservationRepository reservationRepository;

    private static final String KEY_PREFIX = "trip-eleven:session:";

    private enum SyncResult {
        CREATED,
        UPDATED,
        SKIPPED
    }

    public void save(Long accommodationId, String sessionId) {
        redisTemplate.opsForValue().set(
                KEY_PREFIX + accommodationId,
                sessionId
        );
    }

    public String get(Long accommodationId) {
        return redisTemplate.opsForValue().get(
                KEY_PREFIX + accommodationId
        );
    }

    public void delete(Long accommodationId) {
        redisTemplate.delete(
                KEY_PREFIX + accommodationId
        );
    }

    /**
     * 일자별 예약 동기화
     */
    public void sync(String date) {

        tripElevenClient.changeItem("2601");

        List<TripElevenReservationDto> reservations =
                tripElevenClient.getDailyReservations(date);

        int created = 0;
        int updated = 0;
        int skipped = 0;
        int failed = 0;

        log.info(
                "TripEleven 예약 동기화 시작: date={}, count={}",
                date,
                reservations.size()
        );

        for (TripElevenReservationDto dto : reservations) {

            try {
                log.debug(
                        "예약 확인: date={}, reservationId={}, room={}, additionalGuestFeePaid={}",
                        date,
                        dto.getReservationId(),
                        dto.getRoomName(),
                        dto.isAdditionalGuestFeePaid()
                );

                SyncResult result = syncReservation(dto);

                switch (result) {
                    case CREATED -> created++;
                    case UPDATED -> updated++;
                    case SKIPPED -> skipped++;
                }

            } catch (Exception e) {
                failed++;

                log.error(
                        "예약 동기화 실패: reservationId={}",
                        dto.getReservationId(),
                        e
                );
            }
        }

        log.info(
                "TripEleven 동기화 완료 - 조회: {}, 신규: {}, 갱신: {}, 동일: {}, 실패: {}",
                reservations.size(),
                created,
                updated,
                skipped,
                failed
        );
    }

    /**
     * 개별 예약 동기화
     */
    private SyncResult syncReservation(TripElevenReservationDto dto) {

        String reservationId = dto.getReservationId();

        // 1. 예약 상세정보 조회 및 파싱
        TripElevenReservationDetailDto detail =
                tripElevenClient.getReservationDetail(reservationId);

        // 2. DTO -> Reservation
        Reservation incoming = toReservation(dto, detail);

        // 3. 기존 예약 조회
        Optional<Reservation> existing =
                reservationRepository.findByPlatformAndReservationId(
                        Platform.TRIP_ELEVEN,
                        reservationId
                );

        // 4. 신규 예약
        if (existing.isEmpty()) {

            reservationRepository.save(incoming);

            log.info(
                    "신규 예약 저장: reservationId={}",
                    reservationId
            );

            return SyncResult.CREATED;
        }

        Reservation reservation = existing.get();

        // 5. 취소 예약은 자동 갱신하지 않음
        if (reservation.getEventType() == ReservationEventType.CANCELLED) {

            log.warn(
                    "취소된 예약 확인 필요: reservationId={}",
                    reservationId
            );

            return SyncResult.SKIPPED;
        }

        // 6. 기존 예약과 비교
        boolean changed = updateIfChanged(reservation, incoming);

        if (!changed) {

            log.debug(
                    "변경 없음: reservationId={}",
                    reservationId
            );

            return SyncResult.SKIPPED;
        }

        // 7. 변경된 예약 저장
        reservationRepository.save(reservation);

        log.info(
                "예약 갱신: reservationId={}, additionalGuestFeePaid={}",
                reservationId,
                reservation.isAdditionalGuestFeePaid()
        );

        return SyncResult.UPDATED;
    }

    /**
     * DTO -> Reservation 변환
     */
    private Reservation toReservation(
            TripElevenReservationDto dto,
            TripElevenReservationDetailDto detail
    ) {
        Reservation reservation = new Reservation();

        reservation.setPlatform(Platform.TRIP_ELEVEN);
        reservation.setEventType(ReservationEventType.RESERVED);
        reservation.setReservationId(dto.getReservationId());

        reservation.setCheckInDate(detail.checkInDate());
        reservation.setCheckOutDate(detail.checkOutDate());
        reservation.setNights(detail.nights());

        reservation.setCustomerName(dto.getCustomerName());
        reservation.setCustomerPhone(dto.getCustomerPhone());

        // 예약 인원
        reservation.setAdultCount(dto.getAdultCount());
        reservation.setChildCount(dto.getChildCount());
        reservation.setInfantCount(dto.getInfantCount());

        // 추가인원 요금 결제 여부
        reservation.setAdditionalGuestFeePaid(
                dto.isAdditionalGuestFeePaid()
        );

        return reservation;
    }

    /**
     * 기존 예약과 비교 후 변경된 필드만 갱신
     */
    private boolean updateIfChanged(
            Reservation existing,
            Reservation incoming
    ) {

        boolean changed = false;

        if (!Objects.equals(
                existing.getCheckInDate(),
                incoming.getCheckInDate()
        )) {
            existing.setCheckInDate(incoming.getCheckInDate());
            changed = true;
        }

        if (!Objects.equals(
                existing.getCheckOutDate(),
                incoming.getCheckOutDate()
        )) {
            existing.setCheckOutDate(incoming.getCheckOutDate());
            changed = true;
        }

        if (existing.getNights() != incoming.getNights()) {
            existing.setNights(incoming.getNights());
            changed = true;
        }

        if (!Objects.equals(
                existing.getCustomerName(),
                incoming.getCustomerName()
        )) {
            existing.setCustomerName(incoming.getCustomerName());
            changed = true;
        }

        // 성인 인원 비교
        if (existing.getAdultCount() != incoming.getAdultCount()) {
            existing.setAdultCount(incoming.getAdultCount());
            changed = true;
        }

        // 아동 인원 비교
        if (existing.getChildCount() != incoming.getChildCount()) {
            existing.setChildCount(incoming.getChildCount());
            changed = true;
        }

        // 유아 인원 비교
        if (existing.getInfantCount() != incoming.getInfantCount()) {
            existing.setInfantCount(incoming.getInfantCount());
            changed = true;
        }

        // 상세 응답에서 연락처가 확인된 경우에만 갱신
        if (incoming.getCustomerPhone() != null
                && !incoming.getCustomerPhone().isBlank()
                && !Objects.equals(
                existing.getCustomerPhone(),
                incoming.getCustomerPhone()
        )) {

            existing.setCustomerPhone(incoming.getCustomerPhone());
            changed = true;
        }

        // 추가인원 요금 결제 여부
        if (existing.isAdditionalGuestFeePaid()
                != incoming.isAdditionalGuestFeePaid()) {

            existing.setAdditionalGuestFeePaid(
                    incoming.isAdditionalGuestFeePaid()
            );

            changed = true;
        }

        return changed;
    }

    public Document syncToday() {
        return tripElevenClient.getTodayReservation();
    }
}
