package com.campusflow.support.dto;

import com.campusflow.support.entity.SupportTicket;
import com.campusflow.support.entity.TicketStatus;
import com.campusflow.user.dto.UserSummaryResponse;
import java.time.Instant;

/** @param user the user the ticket is with (useful for staff; for the user it is themselves) */
public record TicketResponse(
        Long id,
        String subject,
        TicketStatus status,
        boolean openedByStaff,
        Long relatedReportId,
        UserSummaryResponse user,
        long unreadCount,
        Instant lastMessageAt,
        Instant createdAt,
        Instant closedAt) {

    public static TicketResponse from(SupportTicket ticket, long unreadCount) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getSubject(),
                ticket.getStatus(),
                ticket.isOpenedByStaff(),
                ticket.getRelatedReport() == null ? null : ticket.getRelatedReport().getId(),
                UserSummaryResponse.from(ticket.getUser()),
                unreadCount,
                ticket.getLastMessageAt(),
                ticket.getCreatedAt(),
                ticket.getClosedAt());
    }
}
