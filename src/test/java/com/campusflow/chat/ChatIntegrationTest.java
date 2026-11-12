package com.campusflow.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusflow.support.IntegrationTest;
import com.campusflow.user.entity.Role;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import tools.jackson.databind.JsonNode;

/** Chat over REST plus live delivery over a real WebSocket (STOMP) connection. */
class ChatIntegrationTest extends IntegrationTest {

    private static final long TIMEOUT_SECONDS = 10;

    @LocalServerPort
    private int port;

    @Autowired
    private SimpUserRegistry userRegistry;

    private final List<StompSession> sessions = new ArrayList<>();
    private ThreadPoolTaskScheduler scheduler;

    @BeforeEach
    void startScheduler() {
        scheduler = new ThreadPoolTaskScheduler();
        scheduler.initialize();
    }

    @AfterEach
    void disconnect() {
        sessions.forEach(StompSession::disconnect);
        scheduler.shutdown();
    }

    private long createItem(TestUser owner) throws Exception {
        return read(mockMvc.perform(as(owner, withJson(post("/api/items"), Map.of(
                        "categoryId", 3, "title", "Máy tính Casio fx-580", "description", "Còn bảo hành",
                        "itemType", "SALE", "price", 300000, "condition", "LIKE_NEW"))))
                .andExpect(status().isCreated())).path("id").asLong();
    }

    private StompSession connect(TestUser user) throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new JacksonJsonMessageConverter(jsonMapper));
        client.setTaskScheduler(scheduler);
        StompHeaders connectHeaders = new StompHeaders();
        if (user != null) {
            connectHeaders.add("Authorization", "Bearer " + user.accessToken());
        }
        StompSession session = client.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(),
                connectHeaders, new StompSessionHandlerAdapter() { }).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        sessions.add(session);
        return session;
    }

    /**
     * Subscribes and waits until the server has registered the subscription, so no push is missed.
     * (The simple broker sends no receipts, so the server's session registry is checked instead.)
     */
    private BlockingQueue<JsonNode> subscribe(TestUser user, String destination) throws Exception {
        StompSession session = connect(user);
        BlockingQueue<JsonNode> received = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return JsonNode.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.add((JsonNode) payload);
            }
        });
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        while (!isSubscribed(user, destination)) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Subscription to " + destination + " was not registered");
            }
            Thread.sleep(20);
        }
        return received;
    }

    private boolean isSubscribed(TestUser user, String destination) {
        SimpUser simpUser = userRegistry.getUser(user.id().toString());
        return simpUser != null && simpUser.getSessions().stream()
                .flatMap(session -> session.getSubscriptions().stream())
                .anyMatch(subscription -> subscription.getDestination().equals(destination));
    }

    @Test
    void messageIsDeliveredLiveAndUnreadCountsTrackReading() throws Exception {
        TestUser seller = registerVerifiedUser("Seller");
        TestUser buyer = registerVerifiedUser("Buyer");
        long itemId = createItem(seller);

        BlockingQueue<JsonNode> sellerInbox = subscribe(seller, "/user/queue/messages");

        long conversationId = read(mockMvc.perform(as(buyer, withJson(post("/api/conversations"), Map.of("itemId", itemId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counterpart.id").value(seller.id()))
                .andExpect(jsonPath("$.buyer").value(true))).path("id").asLong();
        // Opening it again returns the same conversation
        mockMvc.perform(as(buyer, withJson(post("/api/conversations"), Map.of("itemId", itemId))))
                .andExpect(jsonPath("$.id").value(conversationId));

        mockMvc.perform(as(buyer, withJson(post("/api/conversations/{id}/messages", conversationId),
                        Map.of("content", "  Máy còn bảo hành không bạn?  "))))
                .andExpect(status().isCreated());

        JsonNode pushed = sellerInbox.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(pushed).as("message pushed to the seller over WebSocket").isNotNull();
        assertThat(pushed.path("content").asString()).isEqualTo("Máy còn bảo hành không bạn?");
        assertThat(pushed.path("senderId").asLong()).isEqualTo(buyer.id());

        mockMvc.perform(as(seller, get("/api/conversations/unread-count"))).andExpect(jsonPath("$.unread").value(1));
        mockMvc.perform(as(buyer, get("/api/conversations/unread-count"))).andExpect(jsonPath("$.unread").value(0));
        mockMvc.perform(as(seller, get("/api/conversations")))
                .andExpect(jsonPath("$.content[0].unreadCount").value(1))
                .andExpect(jsonPath("$.content[0].lastMessagePreview").value("Máy còn bảo hành không bạn?"));

        mockMvc.perform(as(seller, post("/api/conversations/{id}/read", conversationId))).andExpect(status().isNoContent());
        mockMvc.perform(as(seller, get("/api/conversations/unread-count"))).andExpect(jsonPath("$.unread").value(0));
    }

    @Test
    void olderMessagesAreLoadedWithACursor() throws Exception {
        TestUser seller = registerVerifiedUser("Seller");
        TestUser buyer = registerVerifiedUser("Buyer");
        long conversationId = read(mockMvc.perform(as(buyer,
                withJson(post("/api/conversations"), Map.of("itemId", createItem(seller)))))).path("id").asLong();
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(as(i % 2 == 0 ? seller : buyer,
                    withJson(post("/api/conversations/{id}/messages", conversationId), Map.of("content", "m" + i))));
        }

        JsonNode newest = read(mockMvc.perform(as(buyer, get("/api/conversations/{id}/messages", conversationId)
                .param("size", "2"))));
        assertThat(newest.findValuesAsString("content")).containsExactly("m5", "m4");

        long before = newest.get(1).path("id").asLong();
        JsonNode older = read(mockMvc.perform(as(buyer, get("/api/conversations/{id}/messages", conversationId)
                .param("before", String.valueOf(before)).param("size", "10"))));
        assertThat(older.findValuesAsString("content")).containsExactly("m3", "m2", "m1");
    }

    @Test
    void notificationsArePushedLive() throws Exception {
        TestUser owner = registerVerifiedUser("Owner");
        TestUser requester = registerVerifiedUser("Requester");
        long itemId = createItem(owner);
        BlockingQueue<JsonNode> notifications = subscribe(owner, "/user/queue/notifications");

        mockMvc.perform(as(requester, withJson(post("/api/exchange-requests"), Map.of("itemId", itemId))))
                .andExpect(status().isCreated());

        JsonNode pushed = notifications.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(pushed).isNotNull();
        assertThat(pushed.path("type").asString()).isEqualTo("EXCHANGE_REQUESTED");
    }

    @Test
    void supportMessagesArePushedToTheUserAnonymouslyAndToStaffWithSender() throws Exception {
        TestUser user = registerVerifiedUser("Student");
        TestUser moderator = withRole(registerVerifiedUser("Mod"), Role.MODERATOR);
        BlockingQueue<JsonNode> userInbox = subscribe(user, "/user/queue/support");
        BlockingQueue<JsonNode> staffInbox = subscribe(moderator, "/user/queue/support");

        long ticketId = read(mockMvc.perform(as(user, withJson(post("/api/support/tickets"),
                Map.of("subject", "Cần hỗ trợ", "message", "Xin chào ban quản trị"))))).path("id").asLong();
        JsonNode toStaff = staffInbox.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(toStaff).isNotNull();
        assertThat(toStaff.path("senderName").asString()).isEqualTo("Student");

        mockMvc.perform(as(moderator, withJson(post("/api/moderation/support-tickets/{id}/messages", ticketId),
                Map.of("content", "Chào bạn, mình hỗ trợ ngay")))).andExpect(status().isCreated());
        JsonNode toUser = null;
        for (int i = 0; i < 2 && (toUser == null || !toUser.path("fromStaff").asBoolean()); i++) {
            toUser = userInbox.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        assertThat(toUser).isNotNull();
        assertThat(toUser.path("senderName").asString()).isEqualTo("Ban quản trị CampusFlow");
        assertThat(toUser.path("senderId").isNull()).as("moderator identity is not revealed").isTrue();
    }

    @Test
    void onlyParticipantsCanSeeAConversation() throws Exception {
        TestUser seller = registerVerifiedUser("Seller");
        TestUser buyer = registerVerifiedUser("Buyer");
        TestUser stranger = registerVerifiedUser("Stranger");
        long itemId = createItem(seller);
        long conversationId = read(mockMvc.perform(as(buyer,
                withJson(post("/api/conversations"), Map.of("itemId", itemId))))).path("id").asLong();

        mockMvc.perform(as(stranger, get("/api/conversations/{id}/messages", conversationId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(as(stranger, withJson(post("/api/conversations/{id}/messages", conversationId),
                Map.of("content", "spam")))).andExpect(status().isNotFound());
        mockMvc.perform(as(seller, withJson(post("/api/conversations"), Map.of("itemId", itemId))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unverifiedStudentsCannotChat() throws Exception {
        TestUser seller = registerVerifiedUser("Seller");
        TestUser unverified = registerUser("Unverified");

        mockMvc.perform(as(unverified, withJson(post("/api/conversations"), Map.of("itemId", createItem(seller)))))
                .andExpect(status().isForbidden());
    }

    @Test
    void webSocketRequiresAValidToken() {
        assertThatThrownBy(() -> connect(null)).as("no token").isNotNull();
        TestUser forged = new TestUser(1L, "x@student.hcmus.edu.vn", "not-a-jwt", null);
        assertThatThrownBy(() -> connect(forged)).as("invalid token").isNotNull();
    }
}
