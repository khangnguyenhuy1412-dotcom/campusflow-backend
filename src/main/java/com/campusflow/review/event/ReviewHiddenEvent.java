package com.campusflow.review.event;

import com.campusflow.review.entity.Review;

/** A moderator hid a review; its author is told why. */
public record ReviewHiddenEvent(Review review, String reason) {
}
