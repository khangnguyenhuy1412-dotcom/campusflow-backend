package com.campusflow.user.entity;

import com.campusflow.university.entity.University;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "student_id", length = 20)
    private String studentId;

    /** University display name, copied from {@link #university} (kept for fast reads and old rows). */
    @Column(name = "university", length = 150)
    private String universityName;

    /** Detected from the email domain at registration; null when the domain is not known yet. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "university_id")
    private University university;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /** Average review rating (1.00 - 5.00); null until the user receives a first review. */
    @Column(name = "reputation_score", precision = 3, scale = 2)
    private BigDecimal reputationScore;

    @Builder.Default
    @Column(name = "review_count", nullable = false)
    private int reviewCount = 0;

    // Reserved for student-email verification
    @Builder.Default
    @Column(name = "is_verified")
    private boolean verified = false;

    @Builder.Default
    @Column(name = "is_active")
    private boolean active = true;

    /** Set together with active = false when a moderator or admin bans the account. */
    @Column(name = "banned_at")
    private Instant bannedAt;

    @Column(name = "ban_reason", length = 500)
    private String banReason;

    /** Whether important notifications are also sent by email. */
    @Builder.Default
    @Column(name = "email_notifications", nullable = false)
    private boolean emailNotifications = true;

    // Roles are few and always needed to build tokens, so load them eagerly
    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
