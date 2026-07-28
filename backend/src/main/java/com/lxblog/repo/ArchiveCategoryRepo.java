package com.lxblog.repo;

import com.lxblog.entity.ArchiveCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArchiveCategoryRepo extends JpaRepository<ArchiveCategory, Long> {
}
