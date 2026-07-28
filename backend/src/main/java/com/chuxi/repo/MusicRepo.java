package com.chuxi.repo;

import com.chuxi.entity.Music;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MusicRepo extends JpaRepository<Music, Long> {
}
