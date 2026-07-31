package com.chuxi.repo;

import com.chuxi.entity.SiteContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SiteContentRepo extends JpaRepository<SiteContent, Long> {
    Optional<SiteContent> findByContentKey(String contentKey);

    void deleteByContentKey(String contentKey);

    List<SiteContent> findByContentKeyStartingWith(String prefix);

    @Modifying
    @Query(value = "UPDATE site_content SET views = COALESCE(views, 0) + 1 WHERE content_key = :key", nativeQuery = true)
    int bumpViews(@Param("key") String key);
}
