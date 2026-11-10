package com.campusflow.moderation.repository;

import com.campusflow.moderation.entity.Report;
import com.campusflow.moderation.entity.ReportStatus;
import com.campusflow.moderation.entity.ReportTargetType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, Long> {

    @EntityGraph(attributePaths = {"reporter", "targetItem", "targetUser", "handledBy"})
    Optional<Report> findWithDetailsById(Long id);

    /** Moderation queue; a null status means all statuses. */
    @EntityGraph(attributePaths = {"reporter", "targetItem", "targetUser", "handledBy"})
    @Query("select r from Report r where (:status is null or r.status = :status)")
    Page<Report> findQueue(@Param("status") ReportStatus status, Pageable pageable);

    /**
     * Stops one person from filing the same report again and again. (Left join: a path like
     * r.targetItem.id would be an inner join and silently drop user reports, which have no item.)
     */
    @Query("""
            select count(r) > 0 from Report r left join r.targetItem i
            where r.reporter.id = :reporterId and r.status = com.campusflow.moderation.entity.ReportStatus.OPEN
              and r.targetType = :targetType
              and ((:itemId is not null and i.id = :itemId) or (:itemId is null and r.targetUser.id = :userId))
            """)
    boolean existsOpenDuplicate(@Param("reporterId") Long reporterId, @Param("targetType") ReportTargetType targetType,
            @Param("itemId") Long itemId, @Param("userId") Long userId);

    /** Other open reports about the same thing, closed together when a moderator acts. */
    @Query("""
            select r from Report r left join r.targetItem i
            where r.status = com.campusflow.moderation.entity.ReportStatus.OPEN and r.id <> :excludeId
              and ((:itemId is not null and i.id = :itemId)
                   or (:itemId is null and r.targetType = com.campusflow.moderation.entity.ReportTargetType.USER
                       and r.targetUser.id = :userId))
            """)
    List<Report> findOpenForSameTarget(@Param("excludeId") Long excludeId, @Param("itemId") Long itemId,
            @Param("userId") Long userId);

    long countByStatus(ReportStatus status);
}
