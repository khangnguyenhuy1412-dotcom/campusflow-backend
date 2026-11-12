package com.campusflow.comment.entity;

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

/** A public question/answer under a listing. Replies are one level deep (a reply has no replies). */
@Entity
@Table(name = "item_comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /** Null for a top-level comment, otherwise the top-level comment this replies to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ItemComment parent;

    @Column(nullable = false, length = 1000)
    private String content;

    /** Soft delete by the author or the item owner; the placeholder keeps the thread readable. */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    /** Hidden by a moderator. */
    @Builder.Default
    @Column(nullable = false)
    private boolean hidden = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isVisible() {
        return !isDeleted() && !hidden;
    }

    public boolean isWrittenBy(Long userId) {
        return author.getId().equals(userId);
    }
}
