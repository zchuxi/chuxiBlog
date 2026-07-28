package com.chuxi.repo;

import com.chuxi.entity.FriendLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FriendLinkRepository extends JpaRepository<FriendLink, Long> {
    List<FriendLink> findByVisibleTrueOrderBySortIndexAsc();
}
