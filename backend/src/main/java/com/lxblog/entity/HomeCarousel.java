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
@Table(name = "home_carousel")
@JsonIgnoreProperties(ignoreUnknown = true)
public class HomeCarousel {
    @Id
    private Long id;
    private String title;
    @Column(length = 500)
    private String description;
    @Column(length = 1000)
    private String content;
    @Column(length = 500)
    private String imageUrl;
    private Integer sortIndex;
    /** 场景编号文案，如 "SCENE 01" */
    private String sceneLabel;
    /** 顶部小标语，如 "PERSPECTIVE" */
    private String kicker;
    /** 角标文案，如 "04/19" */
    private String badge;
    /** 前台是否展示，历史数据 null 视为展示 */
    private Boolean visible = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
