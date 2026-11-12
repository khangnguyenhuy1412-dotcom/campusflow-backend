package com.campusflow.review.entity;

import com.campusflow.exchange.entity.ExchangeRequest;
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

/** A rating one participant gives the other after a completed exchange (one per participant). */
@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exchange_request_id", nullable = false)
    private ExchangeRequest exchangeRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewee_id", nullable = false)
    private User reviewee;

    @Column(nullable = false)
    private int rating;

    @Column(length = 1000)
    private String comment;

    /** The reviewed person's public answer, e.g. their side of the story after a 1-star review. */
    @Column(length = 1000)
    private String reply;

    @Column(name = "replied_at")
    private Instant repliedAt;

    /** Hidden by a moderator (e.g. a false review): not shown and not counted in the reputation score. */
    @Builder.Default
    @Column(nullable = false)
    private boolean hidden = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
