package com.campusflow.exchange.entity;

import com.campusflow.item.entity.Item;
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
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "exchange_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The item being asked for. Its owner decides on the request. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    /** Item the requester offers in return; only for EXCHANGE listings. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offered_item_id")
    private Item offeredItem;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExchangeStatus status = ExchangeStatus.PENDING;

    /** Optimistic lock: e.g. an accept and a cancel at the same moment cannot both succeed. */
    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public User getOwner() {
        return item.getOwner();
    }

    public boolean isOwner(Long userId) {
        return item.isOwnedBy(userId);
    }

    public boolean isRequester(Long userId) {
        return requester.getId().equals(userId);
    }

    public boolean isParticipant(Long userId) {
        return isOwner(userId) || isRequester(userId);
    }

    /** The other side of the deal, seen from the given participant. */
    public User counterpartOf(Long userId) {
        return isOwner(userId) ? requester : getOwner();
    }
}
