package com.campusflow.support.entity;

import com.campusflow.moderation.entity.Report;
import com.campusflow.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * A conversation between one user and the moderation team (all moderators and admins), e.g. to
 * clarify a disputed review. Opened by the user, or by staff who need information from the user.
 */
@Entity
@Table(name = "support_tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String subject;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status = TicketStatus.OPEN;

    @Builder.Default
    @Column(name = "opened_by_staff", nullable = false)
    private boolean openedByStaff = false;

    /** Optional report this ticket is about (e.g. a reported review being clarified). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_report_id")
    private Report relatedReport;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "user_last_read_message_id")
    private Long userLastReadMessageId;

    /** Shared by the whole team: once any staff member reads a ticket, it is read for the team. */
    @Column(name = "staff_last_read_message_id")
    private Long staffLastReadMessageId;

    @Column(name = "closed_at")
    private Instant closedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public boolean isOpen() {
        return status == TicketStatus.OPEN;
    }

    public boolean belongsTo(Long userId) {
        return user.getId().equals(userId);
    }

    public void markReadUpTo(boolean staff, Long messageId) {
        if (staff) {
            staffLastReadMessageId = max(staffLastReadMessageId, messageId);
        } else {
            userLastReadMessageId = max(userLastReadMessageId, messageId);
        }
    }

    private static Long max(Long current, Long candidate) {
        return current == null || candidate > current ? candidate : current;
    }
}
