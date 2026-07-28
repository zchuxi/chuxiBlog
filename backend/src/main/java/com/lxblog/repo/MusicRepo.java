package com.lxblog.repo;

import com.lxblog.entity.Music;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MusicRepo extends JpaRepository<Music, Long> {
}
