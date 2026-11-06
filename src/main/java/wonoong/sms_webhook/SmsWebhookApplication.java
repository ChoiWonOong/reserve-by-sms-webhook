package wonoong.sms_webhook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SmsWebhookApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmsWebhookApplication.class, args);
	}

}
