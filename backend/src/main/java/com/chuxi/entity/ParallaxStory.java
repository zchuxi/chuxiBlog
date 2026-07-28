package com.chuxi.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "parallax_story")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParallaxStory {
    @Id
    private Long id;
    private String title;
    @Column(length = 1000)
    private String description;
    @Column(length = 1000)
    private String note;
    private String align;
    @Column(length = 500)
    private String imageUrl;
    private Integer sortIndex;
}
