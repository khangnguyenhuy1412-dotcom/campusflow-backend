package com.campusflow.wanted.repository;

import com.campusflow.wanted.entity.WantedPost;
import com.campusflow.wanted.entity.WantedStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WantedPostRepository extends JpaRepository<WantedPost, Long> {

    @EntityGraph(attributePaths = {"user", "category"})
    Optional<WantedPost> findWithDetailsById(Long id);

    /**
     * Public "Cần tìm" board: open posts of active users, optionally filtered. Left joins, because
     * a path like w.category.id would be an inner join and drop posts without a category.
     */
    @EntityGraph(attributePaths = {"user", "category"})
    @Query("""
            select w from WantedPost w left join w.category c left join w.user.university u
            where w.status = com.campusflow.wanted.entity.WantedStatus.ACTIVE and w.expiresAt > :now
              and w.user.active = true
              and (:categoryId is null or c.id = :categoryId)
              and (:universityId is null or u.id = :universityId)
            """)
    Page<WantedPost> findBoard(@Param("now") Instant now, @Param("categoryId") Long categoryId,
            @Param("universityId") Long universityId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "category"})
    Page<WantedPost> findByUserId(Long userId, Pageable pageable);

    long countByUserIdAndStatusAndExpiresAtAfter(Long userId, WantedStatus status, Instant now);

    /**
     * Open posts that could match a new item: same category or no category, not the item owner's
     * own posts. Keyword, price and university checks are done in Java (see WantedMatchService).
     */
    @EntityGraph(attributePaths = {"user", "user.university"})
    @Query("""
            select w from WantedPost w left join w.category c
            where w.status = com.campusflow.wanted.entity.WantedStatus.ACTIVE and w.expiresAt > :now
              and w.user.active = true and w.user.id <> :ownerId
              and (c.id is null or c.id = :categoryId)
            """)
    List<WantedPost> findCandidatesFor(@Param("now") Instant now, @Param("ownerId") Long ownerId,
            @Param("categoryId") Long categoryId);

    /** Records that a post was told about an item. Returns 1 the first time, 0 if already recorded. */
    @Modifying
    @Query(value = "INSERT IGNORE INTO wanted_matches (wanted_post_id, item_id) VALUES (:postId, :itemId)",
            nativeQuery = true)
    int recordMatch(@Param("postId") Long postId, @Param("itemId") Long itemId);

    /** Housekeeping: closes posts whose time ran out. */
    @Modifying
    @Query("""
            update WantedPost w set w.status = com.campusflow.wanted.entity.WantedStatus.CLOSED
            where w.status = com.campusflow.wanted.entity.WantedStatus.ACTIVE and w.expiresAt < :now
            """)
    int closeExpired(@Param("now") Instant now);
}
