package com.lxblog.repo;

import com.lxblog.entity.BangumiRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BangumiRecordRepo extends JpaRepository<BangumiRecord, Long> {
    java.util.Optional<BangumiRecord> findBySubjectId(Long subjectId);
}
