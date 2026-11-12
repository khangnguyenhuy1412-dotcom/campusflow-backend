package com.campusflow.review.service;

import com.campusflow.common.dto.PageQuery;
import com.campusflow.common.dto.PageResponse;
import com.campusflow.common.exception.ApiException;
import com.campusflow.exchange.entity.ExchangeRequest;
import com.campusflow.exchange.entity.ExchangeStatus;
import com.campusflow.exchange.repository.ExchangeRequestRepository;
import com.campusflow.review.dto.ReviewRequest;
import com.campusflow.review.dto.ReviewResponse;
import com.campusflow.review.entity.Review;
import com.campusflow.review.event.ReviewCreatedEvent;
import com.campusflow.review.event.ReviewRepliedEvent;
import com.campusflow.review.repository.RatingStats;
import com.campusflow.review.repository.ReviewRepository;
import com.campusflow.user.entity.User;
import com.campusflow.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");

    private final ReviewRepository reviewRepository;
    private final ExchangeRequestRepository exchangeRequestRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher events;

    /** Each participant of a COMPLETED exchange can review the other participant once. */
    @Transactional
    public ReviewResponse create(Long userId, Long exchangeRequestId, ReviewRequest request) {
        ExchangeRequest exchange = exchangeRequestRepository.findWithDetailsById(exchangeRequestId)
                .filter(found -> found.isParticipant(userId))
                .orElseThrow(() -> ApiException.notFound("Exchange request not found"));
        if (exchange.getStatus() != ExchangeStatus.COMPLETED) {
            throw ApiException.conflict("You can only review a completed exchange");
        }
        if (reviewRepository.existsByExchangeRequestIdAndReviewerId(exchangeRequestId, userId)) {
            throw ApiException.conflict("You have already reviewed this exchange");
        }

        User reviewer = userRepository.getReferenceById(userId);
        User reviewee = exchange.counterpartOf(userId);
        Review review = reviewRepository.save(Review.builder()
                .exchangeRequest(exchange)
                .reviewer(reviewer)
                .reviewee(reviewee)
                .rating(request.rating())
                .comment(request.comment() == null || request.comment().isBlank() ? null : request.comment().trim())
                .build());
        reviewRepository.flush();
        recalculateReputation(reviewee);
        events.publishEvent(new ReviewCreatedEvent(review));
        return ReviewResponse.from(review);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> listForUser(Long userId, Integer page, Integer size) {
        return PageResponse.of(
                reviewRepository.findByRevieweeIdAndHiddenFalse(userId, PageQuery.of(page, size, NEWEST_FIRST)),
                ReviewResponse::from);
    }

    /**
     * The reviewed person answers a review publicly, once. The answer cannot be edited, so it
     * cannot be changed after the reviewer has read it.
     */
    @Transactional
    public ReviewResponse reply(Long userId, Long reviewId, String reply) {
        Review review = reviewRepository.findWithDetailsById(reviewId)
                .filter(found -> !found.isHidden())
                .orElseThrow(() -> ApiException.notFound("Review not found"));
        if (!review.getReviewee().getId().equals(userId)) {
            throw ApiException.forbidden("Only the reviewed person can reply to a review");
        }
        if (review.getReply() != null) {
            throw ApiException.conflict("You have already replied to this review");
        }
        review.setReply(reply.strip());
        review.setRepliedAt(Instant.now());
        events.publishEvent(new ReviewRepliedEvent(review));
        return ReviewResponse.from(review);
    }

    /** Recomputes from all visible reviews instead of adjusting incrementally, so the score can never drift. */
    @Transactional
    public void recalculateReputation(User user) {
        RatingStats stats = reviewRepository.ratingStats(user.getId());
        user.setReputationScore(stats.average() == null
                ? null
                : BigDecimal.valueOf(stats.average()).setScale(2, RoundingMode.HALF_UP));
        user.setReviewCount(stats.count().intValue());
    }
}
