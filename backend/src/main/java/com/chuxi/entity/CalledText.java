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
@Table(name = "called_text")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CalledText {
    @Id
    private Long id;
    private String title;
    private String tag;
    @Column(length = 1000)
    private String content;
    @Column(length = 500)
    private String summary;
    @Column(length = 500)
    private String imageUrl;
    private String readTime;
    private Integer sortIndex;
    @Column(length = 500)
    private String audioUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
