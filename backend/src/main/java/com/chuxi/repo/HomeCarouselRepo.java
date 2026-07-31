package com.chuxi.repo;

import com.chuxi.entity.HomeCarousel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface HomeCarouselRepo extends JpaRepository<HomeCarousel, Long> {
    @Query("SELECT c FROM HomeCarousel c WHERE c.visible = true OR c.visible IS NULL ORDER BY c.sortIndex DESC")
    java.util.List<HomeCarousel> findVisibleOrderBySortIndexDesc();
}
