package com.campusflow.wanted.entity;

import com.campusflow.category.entity.Category;
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
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** A "Cần tìm" post: something a student is looking for. New matching items are notified to them. */
@Entity
@Table(name = "wanted_posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WantedPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Optional: only items of this category match. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, length = 150)
    private String title;

    /** Words that must all appear in a matching item's title or description. */
    @Column(nullable = false, length = 200)
    private String keywords;

    @Column(length = 1000)
    private String description;

    /** Optional: SALE items above this price do not match. */
    @Column(name = "max_price", precision = 12, scale = 2)
    private BigDecimal maxPrice;

    @Builder.Default
    @Column(name = "same_university_only", nullable = false)
    private boolean sameUniversityOnly = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WantedStatus status = WantedStatus.ACTIVE;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public boolean isOpen() {
        return status == WantedStatus.ACTIVE && expiresAt.isAfter(Instant.now());
    }

    /** An ACTIVE post whose time ran out counts as CLOSED, even before housekeeping updates the row. */
    public WantedStatus effectiveStatus() {
        return status == WantedStatus.ACTIVE && !isOpen() ? WantedStatus.CLOSED : status;
    }

    public boolean isOwnedBy(Long userId) {
        return user.getId().equals(userId);
    }
}
