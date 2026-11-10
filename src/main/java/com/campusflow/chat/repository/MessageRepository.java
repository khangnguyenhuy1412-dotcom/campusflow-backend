package com.campusflow.chat.repository;

import com.campusflow.chat.entity.Message;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Newest messages first, older than {@code beforeId} when given. Paging by id (a cursor)
     * instead of page numbers keeps pages stable while new messages arrive.
     */
    @EntityGraph(attributePaths = "sender")
    @Query("""
            select m from Message m
            where m.conversation.id = :conversationId and (:beforeId is null or m.id < :beforeId)
            order by m.id desc
            """)
    List<Message> findPage(@Param("conversationId") Long conversationId, @Param("beforeId") Long beforeId,
            Pageable pageable);

    @Query("select max(m.id) from Message m where m.conversation.id = :conversationId")
    Optional<Long> findLatestId(@Param("conversationId") Long conversationId);
}
