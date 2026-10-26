package com.campusflow.review.repository;

import com.campusflow.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByExchangeRequestIdAndReviewerId(Long exchangeRequestId, Long reviewerId);

    @EntityGraph(attributePaths = {"reviewer", "exchangeRequest", "exchangeRequest.item"})
    Page<Review> findByRevieweeId(Long revieweeId, Pageable pageable);

    @Query("""
            select new com.campusflow.review.repository.RatingStats(avg(r.rating), count(r))
            from Review r where r.reviewee.id = :userId
            """)
    RatingStats ratingStats(@Param("userId") Long userId);
}
