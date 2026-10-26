package com.campusflow.review.repository;

/** Result of {@link ReviewRepository#ratingStats}; average is null when there are no reviews. */
public record RatingStats(Double average, Long count) {
}
