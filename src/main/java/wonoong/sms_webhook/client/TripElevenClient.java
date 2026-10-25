package wonoong.sms_webhook.client;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import wonoong.sms_webhook.Config.TripElevenProperties;
import wonoong.sms_webhook.dto.tripEleven.TripElevenReservationDto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class TripElevenClient {
    private final TripElevenProperties properties;
    private final RestClient restClient;
    private final StringRedisTemplate redisTemplate;
    private static final String SESSION_KEY = "trip-eleven:session:{accommodationId}";

    public TripElevenClient(
            TripElevenProperties properties,
            @Qualifier("tripElevenRestClient") RestClient restClient,
            StringRedisTemplate redisTemplate
    ) {
        this.properties = properties;
        this.restClient = restClient;
        this.redisTemplate = redisTemplate;
    }

    public void login() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        formData.add("provider_id", "1");
        formData.add("userid", properties.getUsername());
        formData.add("passwd", md5(properties.getPassword()));

        ResponseEntity<String> response = restClient.post()
                .uri("/partner/login_submit/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .toEntity(String.class);

        log.info("TripEleven 로그인 응답 상태: {}", response.getStatusCode());

        List<String> setCookies = response.getHeaders()
                .get(HttpHeaders.SET_COOKIE);

        if (setCookies == null || setCookies.isEmpty()) {
            throw new IllegalStateException(
                    "TripEleven 로그인 응답에 Set-Cookie가 없습니다."
            );
        }

        String sessionId = extractSessionId(setCookies);

        if (sessionId.isBlank()) {
            throw new IllegalStateException(
                    "TripEleven sessionid가 비어 있습니다."
            );
        }

        redisTemplate.opsForValue().set(SESSION_KEY, sessionId);

        log.info("TripEleven sessionid를 Redis에 저장했습니다.");
    }

    private String md5(String value) {

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            byte[] digest = md.digest(
                    value.getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder sb = new StringBuilder();

            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 알고리즘을 사용할 수 없습니다.", e);
        }
    }

    private String getSessionId() {

        String sessionId = redisTemplate.opsForValue().get(SESSION_KEY);

        if (sessionId == null || sessionId.isBlank()) {
            login();

            sessionId = redisTemplate.opsForValue().get(SESSION_KEY);
        }

        if (sessionId == null) {
            throw new IllegalStateException("TripEleven sessionid를 가져오지 못했습니다.");
        }

        return sessionId;
    }
    /**
     * sessionid 추출
     */
    private String extractSessionId(List<String> setCookies) {

        for (String setCookie : setCookies) {

            String[] cookies = setCookie.split(";");

            for (String cookie : cookies) {

                String trimmedCookie = cookie.trim();

                if (trimmedCookie.startsWith("sessionid=")) {
                    return trimmedCookie.substring("sessionid=".length());
                }
            }
        }

        throw new IllegalStateException(
                "TripEleven sessionid 쿠키를 찾을 수 없습니다."
        );
    }
    /**
     * @return 당일 예약 정보
     */
    public Document getTodayReservation() {

        String sessionId = getSessionId();

        String html = restClient.get()
                .uri("/partner/room_daily/main/")
                .header(HttpHeaders.COOKIE, "sessionid=" + sessionId)
                .retrieve()
                .body(String.class);

        return Jsoup.parse(html);
    }
    /**
     * 일일 예약 조회
     */
    public Document getDailyReservations(String date) {

        String sessionId = getSessionId();

        String csrfToken = getCsrfToken();

        String url = "/partner/resv/main/"
                + "?csrfmiddlewaretoken=" + csrfToken
                + "&bid=" + properties.getBusinessId()
                + "&room_id="
                + "&room_control_data="
                + "&year=" + date.substring(0, 4)
                + "&month=" + date.substring(5, 7)
                + "&kind=t"
                + "&select_date=" + date
                + "&room_kind="
                + "&close_state="
                + "&close_state_text="
                + "&page=1"
                + "&s_order=";

        String html = restClient.get()
                .uri(url)
                .header(HttpHeaders.COOKIE, "sessionid=" + sessionId)
                .retrieve()
                .body(String.class);

        return Jsoup.parse(html);
    }
    /**
     * 특정 예약 상세 조회
     */
    public Document getReservationDetail(String reservationId) {

        String sessionId = getSessionId();

        String url = "/partner/resv/view2/?bid=" + reservationId;

        String html = restClient.get()
                .uri(url)
                .header(HttpHeaders.COOKIE, "sessionid=" + sessionId)
                .retrieve()
                .body(String.class);

        return Jsoup.parse(html);
    }

    private void checkLogin() {
        getSessionId();
    }
    /*
    csrf 토큰 할당
     */
    private String getCsrfToken() {

        String sessionId = getSessionId();

        String html = restClient.get()
                .uri("/partner/resv/main/?bid=" + properties.getBusinessId())
                .header(HttpHeaders.COOKIE, "sessionid=" + sessionId)
                .retrieve()
                .body(String.class);

        Document document = Jsoup.parse(html);

        return document.select("input[name=csrfmiddlewaretoken]")
                .attr("value");
    }

    public List<TripElevenReservationDto> parseDailyReservations(Document document) {

        List<TripElevenReservationDto> reservations = new ArrayList<>();

        for (Element row : document.select("tr.room_rows")) {

            Elements cells = row.select("> td");

            if (cells.size() < 2) {
                continue;
            }

            String roomName = cells.get(0).text();
            String status = cells.get(1).text();

            // 예약 가능한 객실은 예약번호가 없음
            if (!status.contains("예약완료")) {
                continue;
            }

            String reservationId = cells.get(2).text();
            String customerName = cells.get(3).select("div").first().text();
            String stayInfo = cells.get(4).text();
            String amount = cells.get(5).text();

            Element peopleCell = cells.get(6);

            String peopleText = peopleCell.text();

            int adultCount = extractPeopleCount(peopleText, "성인");
            int childCount = extractPeopleCount(peopleText, "아동");
            int infantCount = extractPeopleCount(peopleText, "유아");

            reservations.add(
                    TripElevenReservationDto.builder()
                            .roomName(roomName)
                            .status(status)
                            .reservationId(reservationId)
                            .customerName(customerName)
                            .stayInfo(stayInfo)
                            .amount(amount)
                            .adultCount(adultCount)
                            .childCount(childCount)
                            .infantCount(infantCount)
                            .build()
            );
        }

        return reservations;
    }
    private int extractPeopleCount(String text, String type) {

        Pattern pattern = Pattern.compile(
                type + "\\s*:?\\s*(\\d+)명"
        );

        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        return 0;
    }
    private String updateRoomState(
            MultiValueMap<String, String> formData
    ) {
        return executeUpdateRoomState(formData, getSessionId());
    }

    private String executeUpdateRoomState(
            MultiValueMap<String, String> formData,
            String sessionId
    ) {
        String result = restClient.post()
                .uri("/partner/room_control/room_state_save_submit/")
                .header(HttpHeaders.COOKIE, "sessionid=" + sessionId)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(String.class);

        if (isSessionExpired(result)) {
            log.warn("TripEleven 세션이 만료되었습니다. 재로그인합니다.");

            redisTemplate.delete(SESSION_KEY);
            login();

            String newSessionId = getSessionId();

            result = restClient.post()
                    .uri("/partner/room_control/room_state_save_submit/")
                    .header(HttpHeaders.COOKIE, "sessionid=" + newSessionId)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(String.class);

            if (isSessionExpired(result)) {
                throw new IllegalStateException(
                        "TripEleven 재로그인 후에도 세션이 유효하지 않습니다."
                );
            }
        }

        return result;
    }

    private boolean isSessionExpired(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return false;
        }

        return responseBody.contains("/partner/login/")
                || responseBody.contains("로그인");
    }
    public String blockRoomDate(
            String bid,
            String roomId,
            LocalDate date,
            String reason,
            String reasonText
    ) {
        String dateString = date.toString();

        String roomControlData = """
            [{"base_date":"%s","room_id":"%s"}]
            """.formatted(dateString, roomId).trim();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("bid", bid);
        formData.add("room_id", roomId);
        formData.add("room_kind", "5");
        formData.add("select_date", dateString);
        formData.add("room_control_data", roomControlData);
        formData.add("close_state", reason);
        formData.add("close_state_text", reasonText);
        String result = updateRoomState(formData);
        log.info("{}", result);
        return result;
    }
    public String unblockRoomDate(
            String bid,
            String roomId,
            LocalDate date
    ) {
        String dateString = date.toString();

        String roomControlData = """
            [{"base_date":"%s","room_id":"%s"}]
            """.formatted(dateString, roomId).trim();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        formData.add("bid", bid);
        formData.add("room_id", roomId);
        formData.add("room_control_data", roomControlData);
        formData.add("select_date", dateString);
        formData.add("room_kind", "1");
        formData.add("page", "1");

        return updateRoomState(formData);
    }
}
