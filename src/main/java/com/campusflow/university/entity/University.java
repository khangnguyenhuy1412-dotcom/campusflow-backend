package com.campusflow.university.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "universities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class University {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "short_name", nullable = false, length = 30)
    private String shortName;

    /** Students' emails end with this domain, e.g. "hcmus.edu.vn" (also matches "student.hcmus.edu.vn"). */
    @Column(name = "email_domain", nullable = false, unique = true, length = 100)
    private String emailDomain;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public boolean matchesDomain(String domain) {
        return domain.equals(emailDomain) || domain.endsWith("." + emailDomain);
    }
}
