package com.chuxi.repo;

import com.chuxi.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMemberRepo extends JpaRepository<TeamMember, Long> {
}
