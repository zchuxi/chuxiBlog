package com.chuxi.repo;

import com.chuxi.entity.Barrage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BarrageRepo extends JpaRepository<Barrage, Long> {
    Page<Barrage> findByApprovedTrue(Pageable pageable);
}
