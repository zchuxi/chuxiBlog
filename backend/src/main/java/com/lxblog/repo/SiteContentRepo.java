package com.lxblog.repo;

import com.lxblog.entity.SiteContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SiteContentRepo extends JpaRepository<SiteContent, Long> {
    Optional<SiteContent> findByContentKey(String contentKey);
}
