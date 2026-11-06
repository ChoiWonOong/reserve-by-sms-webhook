package wonoong.sms_webhook.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import wonoong.sms_webhook.service.TripElevenService;

import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationScheduler {

    private final TripElevenService tripElevenService;

    @Scheduled(
            cron = "0 */10 8-21 * * *",
            zone = "Asia/Seoul"
    )
    public void syncReservations() {
        String today = LocalDate.now(
                ZoneId.of("Asia/Seoul")
        ).toString();

        log.info("TripEleven 예약 동기화 시작: {}", today);

        try {
            tripElevenService.sync(today);
        } catch (Exception e) {
            log.error("TripEleven 예약 동기화 실패", e);
        }
    }
}