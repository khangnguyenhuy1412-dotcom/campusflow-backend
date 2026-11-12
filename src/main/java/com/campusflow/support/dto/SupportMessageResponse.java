package com.campusflow.support.dto;

import com.campusflow.support.entity.SupportMessage;
import java.time.Instant;

/**
 * A message as one side sees it. Users see staff messages as "Ban quản trị CampusFlow" without
 * the moderator's identity (protects moderators); staff see who wrote what.
 */
public record SupportMessageResponse(
        Long id,
        Long ticketId,
        boolean fromStaff,
        Long senderId,
        String senderName,
        String content,
        Instant createdAt) {

    public static final String STAFF_DISPLAY_NAME = "Ban quản trị CampusFlow";

    public static SupportMessageResponse forUser(SupportMessage message) {
        boolean staff = message.isFromStaff();
        return new SupportMessageResponse(
                message.getId(),
                message.getTicket().getId(),
                staff,
                staff ? null : message.getSender().getId(),
                staff ? STAFF_DISPLAY_NAME : message.getSender().getFullName(),
                message.getContent(),
                message.getCreatedAt());
    }

    public static SupportMessageResponse forStaff(SupportMessage message) {
        return new SupportMessageResponse(
                message.getId(),
                message.getTicket().getId(),
                message.isFromStaff(),
                message.getSender().getId(),
                message.getSender().getFullName(),
                message.getContent(),
                message.getCreatedAt());
    }
}
