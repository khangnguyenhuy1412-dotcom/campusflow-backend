package com.campusflow.comment.service;

import com.campusflow.block.service.BlockService;
import com.campusflow.comment.dto.CommentRequest;
import com.campusflow.comment.dto.CommentResponse;
import com.campusflow.comment.entity.ItemComment;
import com.campusflow.comment.event.CommentCreatedEvent;
import com.campusflow.comment.repository.ItemCommentRepository;
import com.campusflow.common.dto.PageQuery;
import com.campusflow.common.dto.PageResponse;
import com.campusflow.common.exception.ApiException;
import com.campusflow.item.entity.Item;
import com.campusflow.item.entity.ItemStatus;
import com.campusflow.item.service.ItemService;
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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public Q&A under a listing. Whoever can see the item can read its comments; writing needs a
 * verified email (SecurityConfig) and an item that is still on offer.
 */
@Service
@RequiredArgsConstructor
public class ItemCommentService {

    private static final Set<ItemStatus> OPEN_FOR_COMMENTS = EnumSet.of(ItemStatus.AVAILABLE, ItemStatus.IN_TRANSACTION);
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");

    private final ItemCommentRepository commentRepository;
    private final ItemService itemService;
    private final UserRepository userRepository;
    private final BlockService blockService;
    private final ApplicationEventPublisher events;

    /** Top-level comments newest first, each with all its replies (one query for the replies). */
    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> list(Long itemId, Long viewerId, Integer page, Integer size) {
        itemService.getVisibleItem(itemId, viewerId);
        Page<ItemComment> topLevel = commentRepository.findTopLevel(itemId, PageQuery.of(page, size, NEWEST_FIRST));
        List<Long> ids = topLevel.getContent().stream().map(ItemComment::getId).toList();
        Map<Long, List<CommentResponse>> replies = ids.isEmpty() ? Map.of()
                : commentRepository.findReplies(ids).stream().collect(Collectors.groupingBy(
                        reply -> reply.getParent().getId(),
                        Collectors.mapping(reply -> CommentResponse.from(reply, List.of()), Collectors.toList())));
        return PageResponse.of(topLevel,
                comment -> CommentResponse.from(comment, replies.getOrDefault(comment.getId(), List.of())));
    }

    @Transactional
    public CommentResponse create(Long userId, Long itemId, CommentRequest request) {
        Item item = itemService.getVisibleItem(itemId, userId);
        if (!OPEN_FOR_COMMENTS.contains(item.getStatus())) {
            throw ApiException.conflict("This listing is closed for comments");
        }
        blockService.requireNoBlock(userId, item.getOwner().getId());

        ItemComment parent = null;
        if (request.parentId() != null) {
            parent = commentRepository.findWithDetailsById(request.parentId())
                    .filter(found -> found.getItem().getId().equals(itemId))
                    .orElseThrow(() -> ApiException.notFound("Comment not found"));
            if (parent.getParent() != null) {
                throw ApiException.badRequest("Reply to the original comment; replies cannot be nested");
            }
            if (!parent.isVisible()) {
                throw ApiException.conflict("You cannot reply to a deleted or hidden comment");
            }
            blockService.requireNoBlock(userId, parent.getAuthor().getId());
        }

        ItemComment comment = commentRepository.save(ItemComment.builder()
                .item(item)
                .author(userRepository.getReferenceById(userId))
                .parent(parent)
                .content(request.content().strip())
                .build());
        events.publishEvent(new CommentCreatedEvent(comment));
        return CommentResponse.from(comment, List.of());
    }

    /** The author or the item owner may delete; the comment stays as a "deleted" placeholder. */
    @Transactional
    public void delete(Long userId, Long itemId, Long commentId) {
        ItemComment comment = commentRepository.findWithDetailsById(commentId)
                .filter(found -> found.getItem().getId().equals(itemId))
                .orElseThrow(() -> ApiException.notFound("Comment not found"));
        if (!comment.isWrittenBy(userId) && !comment.getItem().isOwnedBy(userId)) {
            throw ApiException.forbidden("Only the author or the item owner can delete this comment");
        }
        if (!comment.isDeleted()) {
            comment.setDeletedAt(Instant.now());
        }
    }
}
