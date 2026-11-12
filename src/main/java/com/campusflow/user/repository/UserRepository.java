package com.campusflow.user.repository;

import com.campusflow.user.entity.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Active users holding any of the given roles, e.g. the moderation team to push support messages to. */
    @Query("select distinct u.id from User u join u.roles r where r.name in :roleNames and u.active = true")
    List<Long> findActiveIdsWithAnyRole(@Param("roleNames") Collection<String> roleNames);

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
