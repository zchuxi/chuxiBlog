package com.chuxi.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.chuxi.common.R;
import com.chuxi.entity.Article;
import com.chuxi.repo.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        List<Article> all = articleRepo.findAll();
        List<Article> published = all.stream().filter(a -> !"草稿".equals(a.getStatus())).toList();

        long categoryCount = published.stream()
                .map(Article::getCategoryName)
                .filter(c -> c != null && !c.isBlank())
                .distinct().count();
        long tagCount = published.stream()
                .flatMap(a -> Dtos.splitTags(a.getTags()).stream())
                .distinct().count();

        // 已发布文章按 categoryName 汇总
        Map<String, Long> grouped = published.stream()
                .filter(a -> a.getCategoryName() != null && !a.getCategoryName().isBlank())
                .collect(Collectors.groupingBy(Article::getCategoryName, LinkedHashMap::new, Collectors.counting()));
        List<Map<String, Object>> categoryDistribution = grouped.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .map(e -> Map.<String, Object>of("name", e.getKey(), "count", e.getValue()))
                .toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("articleCount", published.size());
        data.put("draftCount", all.size() - published.size());
        data.put("categoryCount", categoryCount);
        data.put("tagCount", tagCount);
        data.put("viewCount", SiteContentController.readViews(siteContentRepo, mapper));
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
