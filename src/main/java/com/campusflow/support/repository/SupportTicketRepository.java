package com.campusflow.support.repository;

import com.campusflow.support.entity.SupportTicket;
import com.campusflow.support.entity.TicketStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    @EntityGraph(attributePaths = {"user", "relatedReport"})
    Optional<SupportTicket> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"user", "relatedReport"})
    @Query("""
            select t from SupportTicket t where t.user.id = :userId
            order by coalesce(t.lastMessageAt, t.createdAt) desc, t.id desc
            """)
    Page<SupportTicket> findForUser(@Param("userId") Long userId, Pageable pageable);

    /** Staff queue, most recently active first; a null status means all tickets. */
    @EntityGraph(attributePaths = {"user", "relatedReport"})
    @Query("""
            select t from SupportTicket t where (:status is null or t.status = :status)
            order by coalesce(t.lastMessageAt, t.createdAt) desc, t.id desc
            """)
    Page<SupportTicket> findQueue(@Param("status") TicketStatus status, Pageable pageable);

    long countByUserIdAndStatus(Long userId, TicketStatus status);

    /** Messages the user has not read yet (those written by staff), per ticket. */
    @Query("""
            select new com.campusflow.support.repository.TicketUnreadCount(t.id, count(m))
            from SupportMessage m join m.ticket t
            where t.id in :ticketIds and m.fromStaff = true and m.id > coalesce(t.userLastReadMessageId, 0)
            group by t.id
            """)
    List<TicketUnreadCount> countUnreadForUser(@Param("ticketIds") Collection<Long> ticketIds);

    /** Messages the staff team has not read yet (those written by the user), per ticket. */
    @Query("""
            select new com.campusflow.support.repository.TicketUnreadCount(t.id, count(m))
            from SupportMessage m join m.ticket t
            where t.id in :ticketIds and m.fromStaff = false and m.id > coalesce(t.staffLastReadMessageId, 0)
            group by t.id
            """)
    List<TicketUnreadCount> countUnreadForStaff(@Param("ticketIds") Collection<Long> ticketIds);
}
