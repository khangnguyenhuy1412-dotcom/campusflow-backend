package com.campusflow.chat.service;

import com.campusflow.chat.dto.ChatUnreadResponse;
import com.campusflow.chat.dto.ConversationResponse;
import com.campusflow.chat.dto.MessageResponse;
import com.campusflow.chat.entity.Conversation;
import com.campusflow.chat.entity.Message;
import com.campusflow.chat.event.ChatMessageSentEvent;
import com.campusflow.chat.repository.ConversationRepository;
import com.campusflow.chat.repository.MessageRepository;
import com.campusflow.chat.repository.UnreadCount;
import com.campusflow.common.dto.PageQuery;
import com.campusflow.common.dto.PageResponse;
import com.campusflow.common.exception.ApiException;
import com.campusflow.item.entity.Item;
import com.campusflow.item.entity.ItemStatus;
import com.campusflow.item.repository.ItemRepository;
import com.campusflow.user.entity.User;
import com.campusflow.user.repository.UserRepository;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Buyer-seller chat about an item. Only the two participants can read or write a conversation;
 * for anyone else it does not exist (404).
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    /** A conversation can be started while the item is still being traded, not after. */
    private static final Set<ItemStatus> CHATTABLE = EnumSet.of(ItemStatus.AVAILABLE, ItemStatus.IN_TRANSACTION);
    private static final int DEFAULT_MESSAGES = 30;
    private static final int MAX_MESSAGES = 50;
    private static final int PREVIEW_LENGTH = 200;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher events;

    /** Opens the buyer's conversation about an item, creating it on first contact. */
    @Transactional
    public ConversationResponse start(Long userId, Long itemId) {
        Item item = itemRepository.findWithDetailsById(itemId)
                .filter(found -> CHATTABLE.contains(found.getStatus()))
                .orElseThrow(() -> ApiException.notFound("Item not found"));
        if (item.isOwnedBy(userId)) {
            throw ApiException.badRequest("You cannot start a chat about your own item");
        }
        Conversation conversation = conversationRepository.findByItemIdAndBuyerId(itemId, userId)
                .orElseGet(() -> conversationRepository.save(Conversation.builder()
                        .item(item)
                        .buyer(userRepository.getReferenceById(userId))
                        .seller(item.getOwner())
                        .build()));
        return toResponse(conversation, userId);
    }

    @Transactional(readOnly = true)
    public PageResponse<ConversationResponse> list(Long userId, Integer page, Integer size) {
        Page<Conversation> conversations = conversationRepository.findForUser(userId, PageQuery.of(page, size, Sort.unsorted()));
        List<Long> ids = conversations.getContent().stream().map(Conversation::getId).toList();
        Map<Long, Long> unread = ids.isEmpty()
                ? Map.of()
                : conversationRepository.countUnread(ids, userId).stream()
                        .collect(Collectors.toMap(UnreadCount::conversationId, UnreadCount::count));
        return PageResponse.of(conversations,
                conversation -> ConversationResponse.from(conversation, userId, unread.getOrDefault(conversation.getId(), 0L)));
    }

    @Transactional(readOnly = true)
    public ConversationResponse get(Long userId, Long conversationId) {
        return toResponse(loadAsParticipant(userId, conversationId), userId);
    }

    /** Newest first; pass the smallest id received as {@code beforeId} to load older messages. */
    @Transactional(readOnly = true)
    public List<MessageResponse> messages(Long userId, Long conversationId, Long beforeId, Integer size) {
        loadAsParticipant(userId, conversationId);
        int limit = size == null ? DEFAULT_MESSAGES : Math.clamp(size, 1, MAX_MESSAGES);
        return messageRepository.findPage(conversationId, beforeId, PageRequest.of(0, limit)).stream()
                .map(MessageResponse::from)
                .toList();
    }

    @Transactional
    public MessageResponse send(Long userId, Long conversationId, String content) {
        Conversation conversation = loadAsParticipant(userId, conversationId);
        User sender = userRepository.getReferenceById(userId);
        String text = content.strip();
        if (text.isEmpty()) {
            throw ApiException.badRequest("Message must not be empty");
        }

        Message message = messageRepository.save(Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(text)
                .build());
        conversation.setLastMessageAt(Instant.now());
        conversation.setLastMessagePreview(preview(text));
        // Your own message is never unread for you
        conversation.markReadUpTo(userId, message.getId());

        MessageResponse response = MessageResponse.from(message);
        events.publishEvent(new ChatMessageSentEvent(response,
                List.of(conversation.getBuyer().getId(), conversation.getSeller().getId())));
        return response;
    }

    @Transactional
    public void markRead(Long userId, Long conversationId) {
        Conversation conversation = loadAsParticipant(userId, conversationId);
        messageRepository.findLatestId(conversationId)
                .ifPresent(latestId -> conversation.markReadUpTo(userId, latestId));
    }

    @Transactional(readOnly = true)
    public ChatUnreadResponse unreadCount(Long userId) {
        return new ChatUnreadResponse(conversationRepository.countAllUnread(userId));
    }

    private ConversationResponse toResponse(Conversation conversation, Long userId) {
        long unread = conversationRepository.countUnread(List.of(conversation.getId()), userId).stream()
                .mapToLong(UnreadCount::count).sum();
        return ConversationResponse.from(conversation, userId, unread);
    }

    private Conversation loadAsParticipant(Long userId, Long conversationId) {
        return conversationRepository.findWithDetailsById(conversationId)
                .filter(conversation -> conversation.isParticipant(userId))
                .orElseThrow(() -> ApiException.notFound("Conversation not found"));
    }

    private static String preview(String text) {
        String singleLine = text.replaceAll("\\s+", " ");
        return singleLine.length() <= PREVIEW_LENGTH ? singleLine : singleLine.substring(0, PREVIEW_LENGTH - 1) + "…";
    }
}
