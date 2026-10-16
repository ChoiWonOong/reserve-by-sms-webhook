package wonoong.sms_webhook.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wonoong.sms_webhook.client.CamfitClient;
import wonoong.sms_webhook.client.TripElevenClient;
import wonoong.sms_webhook.entity.ExternalSite;
import wonoong.sms_webhook.entity.Platform;
import wonoong.sms_webhook.entity.ReservationEvent;
import wonoong.sms_webhook.repository.ExternalSiteRepository;
import wonoong.sms_webhook.repository.SiteRepository;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final TripElevenClient tripElevenClient;
    private final CamfitClient camfitClient;
    private final SiteRepository siteRepository;
    private final ExternalSiteRepository externalSiteRepository;

    @Transactional(readOnly = true)
    public String blockTripElevenRoom(ReservationEvent event) {
        String reasonText = String.format(
                "캠 %s %s",
                event.customerName(),
                event.customerPhone()
        );
        String roomId = externalSiteRepository.findByPlatformAndExternalSiteNameAndActiveTrue(
                event.platform(),
                event.externalSiteName()
        ).map(ExternalSite::getExternalSiteId).orElse(null);
        return tripElevenClient.blockRoomDate(
                "6996",
                roomId,
                event.checkInDate(),
                "그외펜션사유",
                reasonText
        );
    }

    @Transactional(readOnly = true)
    public String blockCamfitRoom(ReservationEvent event) {

        if (event.platform() != Platform.CAMFIT) {
            throw new IllegalArgumentException(
                    "캠핏 예약 이벤트가 아닙니다."
            );
        }

        ExternalSite externalSite = externalSiteRepository
                .findByPlatformAndExternalSiteNameAndActiveTrue(
                        Platform.CAMFIT,
                        event.externalSiteName()
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "등록된 캠핏 사이트를 찾을 수 없습니다: "
                                + event.externalSiteName()
                ));

        String siteId = externalSite.getExternalSiteId();

        ZoneId zoneId = ZoneId.of("Asia/Seoul");

        long startTimestamp = event.checkInDate()
                .atTime(16, 59)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli();

        long endTimestamp = event.checkOutDate()
                .atTime(23, 59)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli();

        String title = String.format(
                "트립 %s %s",
                event.customerName(),
                event.customerPhone()
        );

        return camfitClient.blockSchedule(
                siteId,
                startTimestamp,
                endTimestamp,
                title
        );
    }
    @Transactional(readOnly = true)
    public String unblockCamfitSite(
            String scheduleId,
            String siteId
    ) {
        return camfitClient.releaseSchedule(
                scheduleId,
                siteId
        );
    }
    @Transactional(readOnly = true)
    public String unblockTripElevenRoom(String roomId, LocalDate checkInDate) {
        return tripElevenClient.unblockRoomDate("6996",roomId, checkInDate);
    }
}
