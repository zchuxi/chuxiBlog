package com.lxblog.repo;

import com.lxblog.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMemberRepo extends JpaRepository<TeamMember, Long> {
}
