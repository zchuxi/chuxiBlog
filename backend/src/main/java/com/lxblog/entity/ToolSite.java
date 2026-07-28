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
@Table(name = "tool_site")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ToolSite {
    @Id
    private Long id;
    private String websiteName;
    @Column(length = 500)
    private String websiteDescription;
    @Column(length = 500)
    private String websiteUrl;
    private Long categoryId;
    private String category;
    @Column(length = 500)
    private String iconUrl;
    /** 详情页展示图（截图/配图），可空 */
    @Column(length = 500)
    private String imageUrl;
    private String tags;
    private String highlight;
    private Boolean featured;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
