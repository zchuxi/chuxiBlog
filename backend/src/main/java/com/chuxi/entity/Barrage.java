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
@Table(name = "barrage")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Barrage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String nickname;
    private String mood;
    private Integer likeCount;
    private Boolean liked;
    @Column(length = 500)
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
