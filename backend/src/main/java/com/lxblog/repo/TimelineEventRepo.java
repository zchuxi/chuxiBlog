package com.lxblog.repo;

import com.lxblog.entity.TimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimelineEventRepo extends JpaRepository<TimelineEvent, Long> {
}
