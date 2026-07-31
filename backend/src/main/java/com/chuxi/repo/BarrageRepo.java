package com.chuxi.repo;

import com.chuxi.entity.Barrage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BarrageRepo extends JpaRepository<Barrage, Long> {
    Page<Barrage> findByApprovedTrue(Pageable pageable);

    @Modifying
    @Query("UPDATE Barrage b SET b.liked = :liked, b.likeCount = :likeCount WHERE b.id = :id")
    int updateLike(@Param("id") Long id, @Param("liked") boolean liked, @Param("likeCount") int likeCount);
}
