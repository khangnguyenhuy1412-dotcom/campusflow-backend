package com.campusflow.chat.entity;

import com.campusflow.item.entity.Item;
import com.campusflow.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * A chat between a buyer and the item's owner (seller) about one item.
 * Deliberately not versioned: two people writing at once must both succeed,
 * and the "last message" fields are only a display summary.
 */
@Entity
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "last_message_preview", length = 200)
    private String lastMessagePreview;

    /** Highest message id the buyer has read; messages above it are unread. */
    @Column(name = "buyer_last_read_message_id")
    private Long buyerLastReadMessageId;

    @Column(name = "seller_last_read_message_id")
    private Long sellerLastReadMessageId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public boolean isParticipant(Long userId) {
        return buyer.getId().equals(userId) || seller.getId().equals(userId);
    }

    public User counterpartOf(Long userId) {
        return buyer.getId().equals(userId) ? seller : buyer;
    }

    public void markReadUpTo(Long userId, Long messageId) {
        if (buyer.getId().equals(userId)) {
            buyerLastReadMessageId = max(buyerLastReadMessageId, messageId);
        } else {
            sellerLastReadMessageId = max(sellerLastReadMessageId, messageId);
        }
    }

    private static Long max(Long current, Long candidate) {
        return current == null || candidate > current ? candidate : current;
    }
}
