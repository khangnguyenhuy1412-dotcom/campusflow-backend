package com.campusflow.comment.repository;

import com.campusflow.comment.entity.ItemComment;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemCommentRepository extends JpaRepository<ItemComment, Long> {

    @EntityGraph(attributePaths = {"item", "item.owner", "author", "parent", "parent.author"})
    Optional<ItemComment> findWithDetailsById(Long id);

    /** Top-level comments of an item, newest first (replies are loaded separately). */
    @EntityGraph(attributePaths = "author")
    @Query("select c from ItemComment c where c.item.id = :itemId and c.parent is null")
    Page<ItemComment> findTopLevel(@Param("itemId") Long itemId, Pageable pageable);

    /** All replies of a page of top-level comments, oldest first, in one query. */
    @EntityGraph(attributePaths = {"author", "parent"})
    @Query("select c from ItemComment c where c.parent.id in :parentIds order by c.createdAt asc, c.id asc")
    List<ItemComment> findReplies(@Param("parentIds") Collection<Long> parentIds);
}
