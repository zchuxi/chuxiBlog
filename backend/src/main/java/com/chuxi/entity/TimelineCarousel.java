package com.chuxi.entity;

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
@Table(name = "timeline_carousel")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimelineCarousel {
    @Id
    private Long id;
    private String title;
    @Column(length = 1000)
    private String content;
    @Column(length = 500)
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
