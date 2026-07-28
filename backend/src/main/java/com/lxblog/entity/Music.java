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
@Table(name = "music")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Music {
    @Id
    private Long id;
    private String title;
    private String artist;
    private String album;
    @Column(length = 500)
    private String coverUrl;
    @Column(length = 500)
    private String musicUrl;
    @Column(columnDefinition = "LONGTEXT")
    private String lyric;
    private LocalDateTime updatedAt;
}
