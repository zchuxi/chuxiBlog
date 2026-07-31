package com.chuxi.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "friend_link")
@JsonIgnoreProperties(ignoreUnknown = true)
public class FriendLink {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 128, nullable = false)
    private String siteName;

    @Column(length = 512, nullable = false)
    private String siteUrl;

    @Column(length = 512)
    private String logoUrl;

    @Column(length = 512)
    private String description;

    private Integer sortIndex;

    private Boolean visible;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
