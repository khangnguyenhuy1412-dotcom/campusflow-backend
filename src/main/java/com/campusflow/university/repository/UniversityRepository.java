package com.campusflow.university.repository;

import com.campusflow.university.entity.University;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UniversityRepository extends JpaRepository<University, Long> {

    boolean existsByEmailDomain(String emailDomain);

    boolean existsByEmailDomainAndIdNot(String emailDomain, Long id);

    /**
     * Links users that registered before their university was added. Native SQL because it
     * matches on the email domain; the domain value comes from a validated admin request.
     */
    @Modifying
    @Query(value = """
            UPDATE users SET university_id = :universityId, university = :name
            WHERE university_id IS NULL
              AND (SUBSTRING_INDEX(email, '@', -1) = :domain
                   OR SUBSTRING_INDEX(email, '@', -1) LIKE CONCAT('%.', :domain))
            """, nativeQuery = true)
    int assignUsersByDomain(@Param("universityId") Long universityId, @Param("name") String name,
            @Param("domain") String domain);

    /** Keeps the copied display name in users in sync after a rename. */
    @Modifying
    @Query("update User u set u.universityName = :name where u.university.id = :universityId")
    int renameForUsers(@Param("universityId") Long universityId, @Param("name") String name);

    /** Clears the copied display name before the university is deleted (the FK sets the id to NULL). */
    @Modifying
    @Query("update User u set u.universityName = null where u.university.id = :universityId")
    int clearForUsers(@Param("universityId") Long universityId);
}
