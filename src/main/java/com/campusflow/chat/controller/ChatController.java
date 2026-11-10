package com.campusflow.chat.controller;

import com.campusflow.chat.dto.ChatUnreadResponse;
import com.campusflow.chat.dto.ConversationResponse;
import com.campusflow.chat.dto.MessageResponse;
import com.campusflow.chat.dto.SendMessageRequest;
import com.campusflow.chat.dto.StartConversationRequest;
import com.campusflow.chat.service.ChatService;
import com.campusflow.common.dto.PageResponse;
import com.campusflow.common.web.CurrentUserId;
import com.campusflow.ratelimit.RateLimit;
import com.campusflow.ratelimit.RateLimitPolicy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Chat commands go through REST (validation, rate limits, verified email); new messages are
 * pushed to connected clients over WebSocket at /user/queue/messages.
 */
@Tag(name = "Chat", description = "Buyer-seller conversations about an item; live updates via WebSocket /ws")
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "Open (or create) your conversation with the owner of an item")
    @RateLimit(policy = RateLimitPolicy.CHAT_MESSAGE, key = RateLimit.Key.USER)
    @PostMapping
    public ConversationResponse start(@CurrentUserId Long userId, @Valid @RequestBody StartConversationRequest request) {
        return chatService.start(userId, request.itemId());
    }

    @Operation(summary = "Your conversations, most recently active first, with unread counts")
    @GetMapping
    public PageResponse<ConversationResponse> list(@CurrentUserId Long userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return chatService.list(userId, page, size);
    }

    @Operation(summary = "Total unread messages (for the chat badge)")
    @GetMapping("/unread-count")
    public ChatUnreadResponse unreadCount(@CurrentUserId Long userId) {
        return chatService.unreadCount(userId);
    }

    @Operation(summary = "Conversation details (participants only)")
    @GetMapping("/{id}")
    public ConversationResponse get(@CurrentUserId Long userId, @PathVariable Long id) {
        return chatService.get(userId, id);
    }

    @Operation(summary = "Messages, newest first; pass before=<smallest id received> for older ones")
    @GetMapping("/{id}/messages")
    public List<MessageResponse> messages(@CurrentUserId Long userId, @PathVariable Long id,
            @RequestParam(required = false) Long before,
            @RequestParam(required = false) Integer size) {
        return chatService.messages(userId, id, before, size);
    }

    @Operation(summary = "Send a message; it is pushed to both participants over WebSocket")
    @RateLimit(policy = RateLimitPolicy.CHAT_MESSAGE, key = RateLimit.Key.USER)
    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse send(@CurrentUserId Long userId, @PathVariable Long id,
            @Valid @RequestBody SendMessageRequest request) {
        return chatService.send(userId, id, request.content());
    }

    @Operation(summary = "Mark all messages in a conversation as read")
    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@CurrentUserId Long userId, @PathVariable Long id) {
        chatService.markRead(userId, id);
    }
}
