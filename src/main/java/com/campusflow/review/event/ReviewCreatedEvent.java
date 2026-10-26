package com.campusflow.review.event;

import com.campusflow.review.entity.Review;

/** Published by ReviewService inside the transaction that saved the review. */
public record ReviewCreatedEvent(Review review) {
}
