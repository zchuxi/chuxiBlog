package com.chuxi.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.chuxi.common.R;
import com.chuxi.repo.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 管理后台概览统计 */
@RestController
@RequestMapping("/api/admin")
public class OverviewController {

    private final ArticleRepo articleRepo;
    private final HomeCarouselRepo homeCarouselRepo;
    private final CollapseCardRepo collapseCardRepo;
    private final TimelineEventRepo timelineEventRepo;
    private final ToolSiteRepo toolSiteRepo;
    private final MusicRepo musicRepo;
    private final CommentRepo commentRepo;
    private final BarrageRepo barrageRepo;
    private final BangumiRecordRepo bangumiRecordRepo;
    private final SiteContentRepo siteContentRepo;
    private final ObjectMapper mapper;

    public OverviewController(ArticleRepo articleRepo, HomeCarouselRepo homeCarouselRepo,
                              CollapseCardRepo collapseCardRepo, TimelineEventRepo timelineEventRepo,
                              ToolSiteRepo toolSiteRepo, MusicRepo musicRepo,
                              CommentRepo commentRepo, BarrageRepo barrageRepo,
                              BangumiRecordRepo bangumiRecordRepo, SiteContentRepo siteContentRepo,
                              ObjectMapper mapper) {
        this.articleRepo = articleRepo;
        this.homeCarouselRepo = homeCarouselRepo;
        this.collapseCardRepo = collapseCardRepo;
        this.timelineEventRepo = timelineEventRepo;
        this.toolSiteRepo = toolSiteRepo;
        this.musicRepo = musicRepo;
        this.commentRepo = commentRepo;
        this.barrageRepo = barrageRepo;
        this.bangumiRecordRepo = bangumiRecordRepo;
        this.siteContentRepo = siteContentRepo;
        this.mapper = mapper;
    }

    @GetMapping("/overview")
    @Transactional(readOnly = true)
    public R<Map<String, Object>> overview() {
        long draftCount = articleRepo.countByStatus("草稿");
        long totalCount = articleRepo.count();
        long articleCount = totalCount - draftCount;

        // 分类数 = categoryName 与 archiveCategory 的并集去重
        var categoryNames = articleRepo.findDistinctPublishedCategoryNames();
        var archiveCategories = articleRepo.findDistinctPublishedArchiveCategories();
        var allCategories = new HashSet<String>();
        allCategories.addAll(categoryNames);
        allCategories.addAll(archiveCategories);
        long categoryCount = allCategories.size();

        // 标签数：只取 tags 字段，在 Java 中拆分去重
        long tagCount = articleRepo.findPublishedTags().stream()
                .flatMap(t -> Dtos.splitTags(t).stream())
                .distinct().count();

        // 已发布文章按 categoryName 汇总（数据库级 GROUP BY）
        List<Map<String, Object>> categoryDistribution = articleRepo.findCategoryDistribution().stream()
                .map(row -> Map.<String, Object>of("name", (String) row[0], "count", (Long) row[1]))
                .toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("articleCount", articleCount);
        data.put("draftCount", draftCount);
        data.put("categoryCount", categoryCount);
        data.put("tagCount", tagCount);
        data.put("viewCount", SiteContentController.readViews(siteContentRepo));
        data.put("bangumiCount", bangumiRecordRepo.count());
        data.put("toolCount", toolSiteRepo.count());
        data.put("musicCount", musicRepo.count());
        data.put("carouselCount", homeCarouselRepo.count());
        data.put("collapseCardCount", collapseCardRepo.count());
        data.put("timelineCount", timelineEventRepo.count());
        data.put("commentCount", commentRepo.count());
        data.put("barrageCount", barrageRepo.count());
        data.put("categoryDistribution", categoryDistribution);
        return R.ok(data);
    }
}
