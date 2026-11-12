package com.campusflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusflow.support.IntegrationTest;
import com.campusflow.user.entity.Role;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** Phase 2: university from email domain, "Cần tìm" matching, QR meetup confirmation. */
class StudentFeaturesIntegrationTest extends IntegrationTest {

    private long createItem(TestUser owner, String title, String type, Integer price) throws Exception {
        Map<String, Object> body = new HashMap<>(Map.of("categoryId", 1, "title", title,
                "description", "Còn mới", "itemType", type, "condition", "LIKE_NEW"));
        if (price != null) {
            body.put("price", price);
        }
        return read(mockMvc.perform(as(owner, withJson(post("/api/items"), body))).andExpect(status().isCreated()))
                .path("id").asLong();
    }

    // ------------------------------------------------------------------ universities

    @Test
    void universityIsDetectedFromTheEmailDomain() throws Exception {
        TestUser student = registerVerifiedUser("Hcmus");

        mockMvc.perform(as(student, get("/api/users/me")))
                .andExpect(jsonPath("$.university").value("Trường Đại học Khoa học Tự nhiên, ĐHQG-HCM"));
        mockMvc.perform(get("/api/universities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.shortName == 'HCMUS')].emailDomain").value("hcmus.edu.vn"));
    }

    @Test
    void sameUniversityFilterAndAdminAddedUniversityLinksExistingStudents() throws Exception {
        String unknownEmail = "nam." + System.nanoTime() + "@student.abcuni.edu.vn";
        mockMvc.perform(withJson(post("/api/auth/register"),
                Map.of("email", unknownEmail, "password", PASSWORD, "fullName", "Nam"))).andExpect(status().isCreated());
        JsonNode login = read(mockMvc.perform(withJson(post("/api/auth/login"),
                Map.of("email", unknownEmail, "password", PASSWORD))));
        TestUser unknown = new TestUser(login.path("user").path("id").asLong(), unknownEmail,
                login.path("accessToken").asString(), login.path("refreshToken").asString());
        mockMvc.perform(as(unknown, get("/api/users/me"))).andExpect(jsonPath("$.university").doesNotExist());
        mockMvc.perform(as(unknown, get("/api/items").param("sameUniversity", "true")))
                .andExpect(status().isBadRequest());

        TestUser admin = withRole(registerVerifiedUser("Admin"), Role.ADMIN);
        mockMvc.perform(as(admin, withJson(post("/api/admin/universities"),
                        Map.of("name", "Trường Đại học ABC", "shortName", "ABC", "emailDomain", "abcuni.edu.vn"))))
                .andExpect(status().isCreated());
        mockMvc.perform(as(unknown, get("/api/users/me"))).andExpect(jsonPath("$.university").value("Trường Đại học ABC"));

        // sameUniversity only shows listings from students of my university
        TestUser hcmusSeller = registerVerifiedUser("HcmusSeller");
        long hcmusItem = createItem(hcmusSeller, "Bàn gấp HCMUS " + System.nanoTime(), "GIVEAWAY", null);
        TestUser hcmusBuyer = registerVerifiedUser("HcmusBuyer");
        JsonNode mine = read(mockMvc.perform(as(hcmusBuyer, get("/api/items")
                .param("sameUniversity", "true").param("size", "50"))));
        assertThat(ids(mine.path("content"))).contains(hcmusItem);
        JsonNode other = read(mockMvc.perform(as(login(unknown), get("/api/items").param("sameUniversity", "true"))));
        assertThat(ids(other.path("content"))).doesNotContain(hcmusItem);
    }

    // ------------------------------------------------------------------ want-list

    @Test
    void wantedPostIsNotifiedWhenAMatchingItemIsListed() throws Exception {
        TestUser seeker = registerVerifiedUser("Seeker");
        TestUser seller = registerVerifiedUser("Seller");
        String unique = "k" + System.nanoTime();

        long postId = read(mockMvc.perform(as(seeker, withJson(post("/api/wanted-posts"), Map.of(
                        "title", "Cần giáo trình Giải tích 1", "keywords", "giai tich 1 " + unique,
                        "maxPrice", 60000, "sameUniversityOnly", false))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))).path("id").asLong();
        mockMvc.perform(get("/api/wanted-posts/{id}", postId)).andExpect(status().isOk());

        // Too expensive and wrong volume: no match
        createItem(seller, "Giải tích 1 " + unique, "SALE", 90000);
        createItem(seller, "Giải tích 2 " + unique, "SALE", 30000);
        // Matches: accent-insensitive keywords, price within budget
        long matchId = createItem(seller, "Giáo trình GIẢI TÍCH 1 " + unique, "SALE", 45000);

        JsonNode notification = awaitNotification(seeker, "WANTED_MATCH");
        assertThat(notification.path("itemId").asLong()).isEqualTo(matchId);
        assertThat(mail.awaitEmail(seeker.email(), "Cần tìm").getText()).contains("GIẢI TÍCH 1");
        Thread.sleep(500);
        assertThat(notificationsOfType(seeker, "WANTED_MATCH")).as("only the matching item is notified").hasSize(1);

        JsonNode matches = read(mockMvc.perform(as(seeker, get("/api/wanted-posts/{id}/matches", postId))));
        assertThat(ids(matches)).containsExactly(matchId);
        mockMvc.perform(as(seller, get("/api/wanted-posts/{id}/matches", postId))).andExpect(status().isNotFound());

        mockMvc.perform(as(seeker, withJson(post("/api/wanted-posts/{id}/close", postId), Map.of("fulfilled", true))))
                .andExpect(jsonPath("$.status").value("FULFILLED"));
    }

    // ------------------------------------------------------------------ QR handover

    @Test
    void meetupCodeFromQrCompletesTheDealAsVerified() throws Exception {
        TestUser giver = registerVerifiedUser("Giver");
        TestUser receiver = registerVerifiedUser("Receiver");
        long itemId = createItem(giver, "Quạt mini " + System.nanoTime(), "GIVEAWAY", null);
        long requestId = read(mockMvc.perform(as(receiver, withJson(post("/api/exchange-requests"),
                Map.of("itemId", itemId))))).path("id").asLong();
        mockMvc.perform(as(giver, post("/api/exchange-requests/{id}/accept", requestId))).andExpect(status().isOk());

        mockMvc.perform(as(receiver, post("/api/exchange-requests/{id}/handover-code", requestId)))
                .andExpect(status().isForbidden());
        JsonNode code = read(mockMvc.perform(as(giver, post("/api/exchange-requests/{id}/handover-code", requestId)))
                .andExpect(status().isOk()));
        assertThat(Instant.parse(code.path("expiresAt").asString()))
                .isBetween(Instant.now().plus(Duration.ofMinutes(9)), Instant.now().plus(Duration.ofMinutes(11)));

        // The QR image really encodes the payload the receiver's app sends back
        String scanned = decodeQr(code.path("qrImage").asString());
        assertThat(scanned).isEqualTo(code.path("qrPayload").asString()).endsWith(code.path("code").asString());

        mockMvc.perform(as(receiver, withJson(post("/api/exchange-requests/{id}/confirm-handover", requestId),
                Map.of("code", "AAAAAAAA")))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Wrong code, 4 attempt(s) left"));
        mockMvc.perform(as(giver, withJson(post("/api/exchange-requests/{id}/confirm-handover", requestId),
                Map.of("code", scanned)))).andExpect(status().isForbidden());

        mockMvc.perform(as(receiver, withJson(post("/api/exchange-requests/{id}/confirm-handover", requestId),
                        Map.of("code", scanned))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.handoverVerified").value(true));
        mockMvc.perform(get("/api/items/{id}", itemId)).andExpect(jsonPath("$.status").value("COMPLETED"));
        assertThat(notificationsOfType(giver, "EXCHANGE_COMPLETED")).as("the giver is told").hasSize(1);
        // The code cannot be used twice
        mockMvc.perform(as(receiver, withJson(post("/api/exchange-requests/{id}/confirm-handover", requestId),
                Map.of("code", scanned)))).andExpect(status().isConflict());
    }

    private static String decodeQr(String dataUrl) throws Exception {
        byte[] png = Base64.getDecoder().decode(dataUrl.substring(dataUrl.indexOf(',') + 1));
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(
                new RGBLuminanceSource(image.getWidth(), image.getHeight(), pixels)));
        return new QRCodeReader().decode(bitmap).getText();
    }

    /** Ids of the top-level objects only (findValues would also return nested category/owner ids). */
    private static List<Long> ids(JsonNode array) {
        return array.valueStream().map(node -> node.path("id").asLong()).toList();
    }

    private List<JsonNode> notificationsOfType(TestUser user, String type) throws Exception {
        JsonNode content = read(mockMvc.perform(as(user, get("/api/notifications").param("size", "50")))).path("content");
        return content.valueStream().filter(n -> type.equals(n.path("type").asString())).toList();
    }

    /** Want-list matching runs in the background after the item is saved, so wait for it. */
    private JsonNode awaitNotification(TestUser user, String type) throws Exception {
        Instant deadline = Instant.now().plusSeconds(10);
        while (Instant.now().isBefore(deadline)) {
            List<JsonNode> found = notificationsOfType(user, type);
            if (!found.isEmpty()) {
                return found.getFirst();
            }
            Thread.sleep(100);
        }
        throw new AssertionError("No " + type + " notification");
    }
}
