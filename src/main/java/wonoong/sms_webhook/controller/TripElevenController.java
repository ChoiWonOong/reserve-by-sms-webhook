package wonoong.sms_webhook.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wonoong.sms_webhook.dto.tripEleven.TripElevenBlockRequestDto;
import wonoong.sms_webhook.dto.tripEleven.TripElevenRoomStateRequestDto;
import wonoong.sms_webhook.service.TripElevenService;

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

    //    @GetMapping("/sync/today")
//    public ResponseEntity<List<TripElevenReservationDto>> syncToday() {
//
//        List<TripElevenReservationDto> reservations =
//                tripElevenScrapingService.syncToday();
//
//        return ResponseEntity.ok(reservations);
//    }
    @GetMapping("/sync/today")
    public ResponseEntity<String> syncToday() {
        return ResponseEntity.ok(
                tripElevenService.syncToday().html()
        );
    }
    @PostMapping("/block")
    public ResponseEntity<String> block(
            @RequestBody TripElevenBlockRequestDto request
    ) throws Exception {
        return ResponseEntity.ok(
                tripElevenService.blockRoomDate(request)
        );
    }

    @PostMapping("/unblock")
    public ResponseEntity<String> unblock(
            @RequestBody TripElevenRoomStateRequestDto request
    ) throws Exception {
        return ResponseEntity.ok(
                tripElevenService.unblockRoomDate(request)
        );
    }
/**/
}
