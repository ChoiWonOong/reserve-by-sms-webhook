package wonoong.sms_webhook.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    @Value("${trip-eleven.base-url}")
    private String tripElevenBaseUrl;
    @Value("${camfit.base-url}")
    private String camfitBaseUrl;

    @Bean
    public RestClient tripElevenRestClient() {

        return RestClient.builder()
                .baseUrl(tripElevenBaseUrl)
                .build();
    }
    @Bean
    public RestClient camfitRestClient() {
        return RestClient.builder()
                .baseUrl(camfitBaseUrl)
                .build();
    }
}