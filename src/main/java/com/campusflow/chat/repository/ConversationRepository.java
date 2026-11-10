package com.campusflow.chat.repository;

import com.campusflow.chat.entity.Conversation;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @EntityGraph(attributePaths = {"item", "buyer", "seller"})
    Optional<Conversation> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"item", "buyer", "seller"})
    Optional<Conversation> findByItemIdAndBuyerId(Long itemId, Long buyerId);

    /** The user's conversations, most recently active first. */
    @EntityGraph(attributePaths = {"item", "buyer", "seller"})
    @Query("""
            select c from Conversation c
            where c.buyer.id = :userId or c.seller.id = :userId
            order by coalesce(c.lastMessageAt, c.createdAt) desc, c.id desc
            """)
    Page<Conversation> findForUser(@Param("userId") Long userId, Pageable pageable);

    /** Unread messages per conversation for one user, in one query for a whole page. */
    @Query("""
            select new com.campusflow.chat.repository.UnreadCount(c.id, count(m))
            from Message m join m.conversation c
            where c.id in :conversationIds and m.sender.id <> :userId
              and m.id > coalesce(case when c.buyer.id = :userId
                                       then c.buyerLastReadMessageId
                                       else c.sellerLastReadMessageId end, 0)
            group by c.id
            """)
    List<UnreadCount> countUnread(@Param("conversationIds") Collection<Long> conversationIds,
            @Param("userId") Long userId);

    /** Total unread messages across all of the user's conversations (for the chat badge). */
    @Query("""
            select count(m) from Message m join m.conversation c
            where (c.buyer.id = :userId or c.seller.id = :userId) and m.sender.id <> :userId
              and m.id > coalesce(case when c.buyer.id = :userId
                                       then c.buyerLastReadMessageId
                                       else c.sellerLastReadMessageId end, 0)
            """)
    long countAllUnread(@Param("userId") Long userId);
}
