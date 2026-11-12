package com.campusflow.support.event;

import com.campusflow.support.dto.SupportMessageResponse;
import com.campusflow.support.entity.SupportTicket;

/**
 * Published when a ticket message is saved; pushed after commit to the ticket's user (user view)
 * and to all staff (staff view). A staff message also notifies the user.
 */
public record SupportMessageSentEvent(
        SupportTicket ticket,
        SupportMessageResponse userView,
        SupportMessageResponse staffView) {

    public boolean fromStaff() {
        return staffView.fromStaff();
    }
}
