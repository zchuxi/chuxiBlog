package com.chuxi.repo;

import com.chuxi.entity.TimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimelineEventRepo extends JpaRepository<TimelineEvent, Long> {
    java.util.List<TimelineEvent> findAllByOrderByIdAsc();
}
