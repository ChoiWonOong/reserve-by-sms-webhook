package wonoong.sms_webhook.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import wonoong.sms_webhook.service.TripElevenService;

import java.time.LocalDate;

@RestController
@RequestMapping("/trip-eleven")
@RequiredArgsConstructor
public class TripElevenController {
    private final TripElevenService tripElevenService;

    @GetMapping("/sync")
    public ResponseEntity<Void> sync(
            @RequestParam String date
    ) {
        tripElevenService.sync(date);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/sync/today")
    public ResponseEntity<Void> syncToday() {
        LocalDate today = LocalDate.now();

        tripElevenService.sync(today.toString());
        return ResponseEntity.ok().build();
    }
}
