package com.lxblog.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "team_member")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TeamMember {
    @Id
    private Long id;
    private String displayName;
    private String email;
    @Column(length = 500)
    private String avatarUrl;
    private String roleCode;
    private String roleLabel;
    private String position;
    @Column(length = 500)
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
