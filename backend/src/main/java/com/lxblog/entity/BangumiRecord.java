package com.lxblog.entity;

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

/** 番剧记录：数据来自 bgm.tv 条目 + 个人观看进度 */
@Getter
@Setter
@Entity
@Table(name = "bangumi_record")
@JsonIgnoreProperties(ignoreUnknown = true)
public class BangumiRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** bgm.tv 条目 id */
    /** bgm 条目 id，同一部番剧只允许收录一次 */
    @Column(unique = true)
    private Long subjectId;
    private String name;
    private String nameCn;
    @Column(length = 500)
    private String coverUrl;
    private Integer totalEps;
    private Integer watchedEps;
    /** 想看 / 在看 / 看过 / 搁置 / 弃番（旧数据可能为“看完”） */
    private String status;
    /** 个人评分 0-10，可空 */
    private Integer rating;
    /** bgm 站均分，可空 */
    private Double score;
    private String airDate;
    /** 放送形态：TV / 剧场版 / OVA 等 */
    private String platform;
    /** bgm 站排名，可空（rank 为 MySQL 保留字，列名改用 bgm_rank） */
    @Column(name = "bgm_rank")
    private Integer rank;
    /** bgm 评分人数，可空 */
    private Integer ratingTotal;
    @Column(columnDefinition = "TEXT")
    private String summary;
    /** CSV 存储，API 层转数组 */
    private String tags;
    /** 分类（热血 / 日常 / 奇幻…） */
    private String category;
    /** 排序值，越小越靠前 */
    private Integer sortIndex;
    /** 前台是否展示，默认展示 */
    private Boolean visible;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
