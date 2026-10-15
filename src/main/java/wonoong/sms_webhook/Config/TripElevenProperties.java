package wonoong.sms_webhook.Config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "trip-eleven")
public class TripElevenProperties {
    private String baseUrl;
    private String username;
    private String password;
    private String businessId;
}