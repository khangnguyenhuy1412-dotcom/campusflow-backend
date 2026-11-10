package com.campusflow.block.repository;

import com.campusflow.block.entity.UserBlock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserBlockRepository extends JpaRepository<UserBlock, UserBlock.Key> {

    @Query("""
            select count(b) > 0 from UserBlock b
            where (b.id.blockerId = :first and b.id.blockedId = :second)
               or (b.id.blockerId = :second and b.id.blockedId = :first)
            """)
    boolean existsEitherWay(@Param("first") Long first, @Param("second") Long second);

    @EntityGraph(attributePaths = "blocked")
    Page<UserBlock> findByIdBlockerId(Long blockerId, Pageable pageable);
}
