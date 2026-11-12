package com.campusflow.review.repository;

import com.campusflow.review.entity.Review;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByExchangeRequestIdAndReviewerId(Long exchangeRequestId, Long reviewerId);

    @EntityGraph(attributePaths = {"reviewer", "reviewee", "exchangeRequest", "exchangeRequest.item"})
    Optional<Review> findWithDetailsById(Long id);

    /** Public reviews of a user; reviews hidden by a moderator are left out. */
    @EntityGraph(attributePaths = {"reviewer", "exchangeRequest", "exchangeRequest.item"})
    Page<Review> findByRevieweeIdAndHiddenFalse(Long revieweeId, Pageable pageable);

    /** Hidden reviews do not count towards the reputation score. */
    @Query("""
            select new com.campusflow.review.repository.RatingStats(avg(r.rating), count(r))
            from Review r where r.reviewee.id = :userId and r.hidden = false
            """)
    RatingStats ratingStats(@Param("userId") Long userId);
}
