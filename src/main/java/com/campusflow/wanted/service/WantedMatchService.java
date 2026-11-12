package com.campusflow.wanted.service;

import com.campusflow.block.repository.UserBlockRepository;
import com.campusflow.item.entity.Item;
import com.campusflow.item.entity.ItemStatus;
import com.campusflow.item.entity.ItemType;
import com.campusflow.item.repository.ItemRepository;
import com.campusflow.university.entity.University;
import com.campusflow.user.entity.User;
import com.campusflow.wanted.entity.WantedPost;
import com.campusflow.wanted.event.ItemPublishedEvent;
import com.campusflow.wanted.event.WantedMatchFoundEvent;
import com.campusflow.wanted.repository.WantedPostRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Matches new listings against open "Cần tìm" posts and notifies each post owner once per item.
 * Candidate posts are narrowed in SQL (status, category); keywords, price, university and blocks
 * are checked here so the accent-insensitive rules are the same everywhere.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WantedMatchService {

    private final WantedPostRepository wantedPostRepository;
    private final ItemRepository itemRepository;
    private final UserBlockRepository userBlockRepository;
    private final ApplicationEventPublisher events;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onItemPublished(ItemPublishedEvent event) {
        itemRepository.findWithDetailsById(event.itemId())
                .filter(item -> item.getStatus() == ItemStatus.AVAILABLE)
                .ifPresent(this::notifyMatchingPosts);
    }

    void notifyMatchingPosts(Item item) {
        String itemText = itemText(item);
        List<WantedPost> candidates = wantedPostRepository.findCandidatesFor(
                Instant.now(), item.getOwner().getId(), item.getCategory().getId());
        int notified = 0;
        for (WantedPost post : candidates) {
            if (matches(post, item, itemText)
                    && !userBlockRepository.existsEitherWay(post.getUser().getId(), item.getOwner().getId())
                    && wantedPostRepository.recordMatch(post.getId(), item.getId()) == 1) {
                events.publishEvent(new WantedMatchFoundEvent(post, item));
                notified++;
            }
        }
        log.debug("Item {} matched {} wanted post(s)", item.getId(), notified);
    }

    /** Whether an item satisfies a post. {@code normalizedItemText} comes from {@link #itemText}. */
    static boolean matches(WantedPost post, Item item, String normalizedItemText) {
        if (post.getCategory() != null && !post.getCategory().getId().equals(item.getCategory().getId())) {
            return false;
        }
        if (post.getMaxPrice() != null && item.getItemType() == ItemType.SALE
                && item.getPrice().compareTo(post.getMaxPrice()) > 0) {
            return false;
        }
        if (post.isSameUniversityOnly() && !sameUniversity(post.getUser(), item.getOwner())) {
            return false;
        }
        return KeywordMatcher.matches(KeywordMatcher.keywords(post.getKeywords()), normalizedItemText);
    }

    static String itemText(Item item) {
        return KeywordMatcher.normalize(item.getTitle() + " " + item.getDescription());
    }

    private static boolean sameUniversity(User first, User second) {
        Long firstId = universityId(first);
        return firstId != null && Objects.equals(firstId, universityId(second));
    }

    private static Long universityId(User user) {
        University university = user.getUniversity();
        return university == null ? null : university.getId();
    }
}
