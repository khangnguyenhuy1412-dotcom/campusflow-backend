package com.campusflow.user.repository;

import com.campusflow.user.entity.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Admin user search by email or name. {@code pattern} comes from SqlLike.contains (wildcards
     * escaped); null means all users. {@code active} null means both active and banned users.
     */
    @Query("""
            select u from User u
            where (:pattern is null or lower(u.email) like :pattern escape '\\'
                   or lower(u.fullName) like :pattern escape '\\')
              and (:active is null or u.active = :active)
            """)
    Page<User> search(@Param("pattern") String pattern, @Param("active") Boolean active, Pageable pageable);
}
