package com.campusflow.notification.repository;

import com.campusflow.notification.entity.Notification;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @EntityGraph(attributePaths = {"actor", "item", "exchangeRequest", "supportTicket"})
    Page<Notification> findByRecipientId(Long recipientId, Pageable pageable);

    @EntityGraph(attributePaths = {"actor", "item", "exchangeRequest", "supportTicket"})
    Page<Notification> findByRecipientIdAndReadAtIsNull(Long recipientId, Pageable pageable);

    /** Looks a notification up only among the user's own, so nobody can touch someone else's. */
    Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId);

    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    @Modifying
    @Query("update Notification n set n.readAt = :now where n.recipient.id = :recipientId and n.readAt is null")
    int markAllRead(@Param("recipientId") Long recipientId, @Param("now") Instant now);

    @Modifying
    @Query("delete from Notification n where n.readAt is not null and n.createdAt < :cutoff")
    int deleteReadBefore(@Param("cutoff") Instant cutoff);
}
