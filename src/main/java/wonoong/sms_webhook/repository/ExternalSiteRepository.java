package wonoong.sms_webhook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wonoong.sms_webhook.entity.ExternalSite;
import wonoong.sms_webhook.entity.Platform;

import java.util.Optional;

public interface ExternalSiteRepository extends JpaRepository<ExternalSite, Long> {
    Optional<ExternalSite> findByPlatformAndExternalSiteNameAndActiveTrue(
            Platform platform,
            String externalSiteId
    );
    Optional<ExternalSite> findBySite_IdAndPlatformAndActiveTrue(
            Long siteId,
            Platform platform
    );
}
