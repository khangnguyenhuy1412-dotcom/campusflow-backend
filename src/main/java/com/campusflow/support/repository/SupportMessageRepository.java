package com.campusflow.support.repository;

import com.campusflow.support.entity.SupportMessage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {

    /** Newest first, older than {@code beforeId} when given (cursor paging, like chat). */
    @EntityGraph(attributePaths = "sender")
    @Query("""
            select m from SupportMessage m
            where m.ticket.id = :ticketId and (:beforeId is null or m.id < :beforeId)
            order by m.id desc
            """)
    List<SupportMessage> findPage(@Param("ticketId") Long ticketId, @Param("beforeId") Long beforeId,
            Pageable pageable);

    @Query("select max(m.id) from SupportMessage m where m.ticket.id = :ticketId")
    Optional<Long> findLatestId(@Param("ticketId") Long ticketId);
}
