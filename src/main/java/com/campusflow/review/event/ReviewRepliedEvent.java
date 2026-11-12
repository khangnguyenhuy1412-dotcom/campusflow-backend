package com.campusflow.review.event;

import com.campusflow.review.entity.Review;

/** The reviewed person answered a review; the reviewer is told. */
public record ReviewRepliedEvent(Review review) {
}
