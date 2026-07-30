package com.chuxi.repo;

import com.chuxi.entity.SiteContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SiteContentRepo extends JpaRepository<SiteContent, Long> {
    Optional<SiteContent> findByContentKey(String contentKey);

    void deleteByContentKey(String contentKey);

    List<SiteContent> findByContentKeyStartingWith(String prefix);
}
