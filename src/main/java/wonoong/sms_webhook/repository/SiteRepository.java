package wonoong.sms_webhook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wonoong.sms_webhook.entity.Site;

public interface SiteRepository extends JpaRepository<Site, Long> {
}
