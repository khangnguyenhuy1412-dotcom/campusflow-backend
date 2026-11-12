package com.campusflow.wanted.service;

import com.campusflow.category.service.CategoryService;
import com.campusflow.common.dto.PageQuery;
import com.campusflow.common.dto.PageResponse;
import com.campusflow.common.exception.ApiException;
import com.campusflow.item.dto.ItemSummaryResponse;
import com.campusflow.item.entity.Item;
import com.campusflow.item.entity.ItemStatus;
import com.campusflow.item.repository.ItemRepository;
import com.campusflow.user.repository.UserRepository;
import com.campusflow.wanted.dto.WantedPostRequest;
import com.campusflow.wanted.dto.WantedPostResponse;
import com.campusflow.wanted.entity.WantedPost;
import com.campusflow.wanted.entity.WantedStatus;
import com.campusflow.wanted.repository.WantedPostRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WantedPostService {

    /** Posts expire so the board does not fill up with requests nobody cares about any more. */
    static final Duration LIFETIME = Duration.ofDays(30);
    static final int MAX_OPEN_POSTS_PER_USER = 10;
    /** How many recent listings are checked when a student opens the matches of their post. */
    private static final int MATCH_SCAN_LIMIT = 500;
    private static final int MAX_MATCHES_RETURNED = 20;
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");

    private final WantedPostRepository wantedPostRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final CategoryService categoryService;

    @Transactional
    public WantedPostResponse create(Long userId, WantedPostRequest request) {
        if (wantedPostRepository.countByUserIdAndStatusAndExpiresAtAfter(userId, WantedStatus.ACTIVE, Instant.now())
                >= MAX_OPEN_POSTS_PER_USER) {
            throw ApiException.conflict("You can have at most " + MAX_OPEN_POSTS_PER_USER + " open posts");
        }
        WantedPost post = WantedPost.builder()
                .user(userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User not found")))
                .expiresAt(Instant.now().plus(LIFETIME))
                .build();
        apply(post, request);
        return WantedPostResponse.from(wantedPostRepository.save(post));
    }

    @Transactional
    public WantedPostResponse update(Long userId, Long postId, WantedPostRequest request) {
        WantedPost post = getOwnedOpenPost(userId, postId);
        apply(post, request);
        return WantedPostResponse.from(post);
    }

    @Transactional
    public WantedPostResponse close(Long userId, Long postId, boolean fulfilled) {
        WantedPost post = getOwnedOpenPost(userId, postId);
        post.setStatus(fulfilled ? WantedStatus.FULFILLED : WantedStatus.CLOSED);
        return WantedPostResponse.from(post);
    }

    @Transactional(readOnly = true)
    public WantedPostResponse get(Long postId) {
        return WantedPostResponse.from(wantedPostRepository.findWithDetailsById(postId)
                .filter(post -> post.getUser().isActive())
                .orElseThrow(WantedPostService::notFound));
    }

    @Transactional(readOnly = true)
    public PageResponse<WantedPostResponse> board(Long categoryId, Long universityId, Integer page, Integer size) {
        return PageResponse.of(
                wantedPostRepository.findBoard(Instant.now(), categoryId, universityId, PageQuery.of(page, size, NEWEST_FIRST)),
                WantedPostResponse::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<WantedPostResponse> mine(Long userId, Integer page, Integer size) {
        return PageResponse.of(wantedPostRepository.findByUserId(userId, PageQuery.of(page, size, NEWEST_FIRST)),
                WantedPostResponse::from);
    }

    /**
     * Available listings that match the post right now (newest first), so the student does not
     * have to wait for a new listing. Only the post owner can see this.
     */
    @Transactional(readOnly = true)
    public List<ItemSummaryResponse> currentMatches(Long userId, Long postId) {
        WantedPost post = wantedPostRepository.findWithDetailsById(postId)
                .filter(found -> found.isOwnedBy(userId))
                .orElseThrow(WantedPostService::notFound);
        Specification<Item> candidates = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), ItemStatus.AVAILABLE));
            predicates.add(cb.isTrue(root.get("owner").get("active")));
            predicates.add(cb.notEqual(root.get("owner").get("id"), userId));
            if (post.getCategory() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), post.getCategory().getId()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return itemRepository.findAll(candidates, PageRequest.of(0, MATCH_SCAN_LIMIT, NEWEST_FIRST)).stream()
                .filter(item -> WantedMatchService.matches(post, item, WantedMatchService.itemText(item)))
                .limit(MAX_MATCHES_RETURNED)
                .map(ItemSummaryResponse::from)
                .toList();
    }

    private void apply(WantedPost post, WantedPostRequest request) {
        if (KeywordMatcher.keywords(request.keywords()).isEmpty()) {
            throw ApiException.badRequest("Give at least one keyword of 2 or more letters");
        }
        if (request.sameUniversityOnly() && post.getUser().getUniversity() == null) {
            throw ApiException.badRequest("Your university is not recognised yet, so 'same university only' is not possible");
        }
        post.setTitle(request.title().strip());
        post.setKeywords(request.keywords().strip());
        post.setDescription(request.description() == null || request.description().isBlank()
                ? null : request.description().strip());
        post.setCategory(request.categoryId() == null ? null : categoryService.getEntity(request.categoryId()));
        post.setMaxPrice(request.maxPrice());
        post.setSameUniversityOnly(request.sameUniversityOnly());
    }

    private WantedPost getOwnedOpenPost(Long userId, Long postId) {
        WantedPost post = wantedPostRepository.findWithDetailsById(postId).orElseThrow(WantedPostService::notFound);
        if (!post.isOwnedBy(userId)) {
            throw ApiException.forbidden("You can only change your own posts");
        }
        if (!post.isOpen()) {
            throw ApiException.conflict("This post is already closed");
        }
        return post;
    }

    private static ApiException notFound() {
        return ApiException.notFound("Post not found");
    }
}
