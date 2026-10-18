package wonoong.sms_webhook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wonoong.sms_webhook.entity.ExternalSite;
import wonoong.sms_webhook.entity.Platform;

import java.util.List;
import java.util.Optional;

public interface ExternalSiteRepository extends JpaRepository<ExternalSite, Long> {
    List<ExternalSite> findBySiteId(Long siteId);

    Optional<ExternalSite> findBySiteIdAndPlatform(
            Long siteId,
            Platform platform
    );

    Optional<ExternalSite> findByPlatformAndExternalSiteNameAndActiveIsTrue(
            Platform platform,
            String externalSiteId
    );
    Optional<ExternalSite> findBySite_IdAndPlatformAndActiveTrue(
            Long siteId,
            Platform platform
    );
}
