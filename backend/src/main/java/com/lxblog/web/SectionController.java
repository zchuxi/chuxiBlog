package com.lxblog.web;

import com.lxblog.common.PageData;
import com.lxblog.common.R;
import com.lxblog.entity.Article;
import com.lxblog.entity.Barrage;
import com.lxblog.repo.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 时间线 / 归档 / 树洞 / 视差 / 工具 / 音乐 */
@RestController
public class SectionController {

    private final TimelineCarouselRepo timelineCarouselRepo;
    private final TimelineEventRepo timelineEventRepo;
    private final ArticleRepo articleRepo;
    private final ArchiveCategoryRepo archiveCategoryRepo;
    private final BarrageRepo barrageRepo;
    private final CalledTextRepo calledTextRepo;
    private final ParallaxStoryRepo parallaxStoryRepo;
    private final ToolSiteRepo toolSiteRepo;
    private final MusicRepo musicRepo;

    public SectionController(TimelineCarouselRepo timelineCarouselRepo, TimelineEventRepo timelineEventRepo,
                             ArticleRepo articleRepo, ArchiveCategoryRepo archiveCategoryRepo,
                             BarrageRepo barrageRepo, CalledTextRepo calledTextRepo,
                             ParallaxStoryRepo parallaxStoryRepo, ToolSiteRepo toolSiteRepo,
                             MusicRepo musicRepo) {
        this.timelineCarouselRepo = timelineCarouselRepo;
        this.timelineEventRepo = timelineEventRepo;
        this.articleRepo = articleRepo;
        this.archiveCategoryRepo = archiveCategoryRepo;
        this.barrageRepo = barrageRepo;
        this.calledTextRepo = calledTextRepo;
        this.parallaxStoryRepo = parallaxStoryRepo;
        this.toolSiteRepo = toolSiteRepo;
        this.musicRepo = musicRepo;
    }

    @GetMapping("/api/front/timeline/landing")
    @Transactional(readOnly = true)
    public R<Map<String, Object>> timelineLanding() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("carousels", timelineCarouselRepo.findAll().stream()
                .sorted(Comparator.comparing(c -> -c.getId())).toList());
        data.put("timelines", timelineEventRepo.findAll().stream()
                .sorted(Comparator.comparing(t -> t.getId())).toList());
        return R.ok(data);
    }

    @GetMapping("/api/front/archive/landing")
    @Transactional(readOnly = true)
    public R<Map<String, Object>> archiveLanding() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("entries", articleRepo.findAll().stream()
                .filter(a -> !"草稿".equals(a.getStatus()))
                .sorted(Comparator.comparing(Article::getPublishedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(Dtos.ArchiveEntry::of).toList());
        data.put("categories", archiveCategoryRepo.findAll().stream().map(c -> Map.of(
                "category", c.getCategory(),
                "title", c.getTitle(),
                "description", c.getDescription(),
                "tags", Dtos.splitTags(c.getTags())
        )).toList());
        return R.ok(data);
    }

    @GetMapping("/api/front/tree-hole/barrages")
    @Transactional(readOnly = true)
    public R<PageData<Barrage>> barrages(@RequestParam(defaultValue = "1") int pageNo,
                                         @RequestParam(defaultValue = "50") int pageSize) {
        var all = barrageRepo.findAll().stream()
                .sorted(Comparator.comparing(Barrage::getId, Comparator.reverseOrder())).toList();
        var page = all.stream().skip((long) (pageNo - 1) * pageSize).limit(pageSize).toList();
        return R.ok(new PageData<>(page, all.size(), pageNo, pageSize));
    }

    @PostMapping("/api/front/tree-hole/barrages")
    @Transactional
    public R<Barrage> addBarrage(@RequestBody Map<String, String> body) {
        String content = body.getOrDefault("content", "").trim();
        if (content.isEmpty()) return R.fail("内容不能为空");
        Barrage b = new Barrage();
        b.setUserId(1L);
        b.setNickname(body.getOrDefault("nickname", "树友-0001"));
        b.setMood(body.getOrDefault("mood", "轻声"));
        b.setContent(content);
        b.setLikeCount(0);
        b.setLiked(false);
        b.setCreatedAt(LocalDateTime.now());
        b.setUpdatedAt(LocalDateTime.now());
        return R.ok(barrageRepo.save(b));
    }

    @PostMapping("/api/front/tree-hole/barrages/{id}/likes")
    @Transactional
    public R<Barrage> likeBarrage(@PathVariable Long id) {
        return barrageRepo.findById(id).map(b -> {
            boolean liked = Boolean.TRUE.equals(b.getLiked());
            b.setLiked(!liked);
            b.setLikeCount(Math.max(0, (b.getLikeCount() == null ? 0 : b.getLikeCount()) + (liked ? -1 : 1)));
            return R.ok(barrageRepo.save(b));
        }).orElseGet(() -> R.fail("弹幕不存在"));
    }

    @GetMapping("/api/front/tree-hole/called-texts")
    @Transactional(readOnly = true)
    public R<PageData<?>> calledTexts(@RequestParam(defaultValue = "1") int pageNo,
                                      @RequestParam(defaultValue = "50") int pageSize) {
        var all = calledTextRepo.findAll().stream()
                .sorted(Comparator.comparing(c -> -(c.getSortIndex() == null ? 0 : c.getSortIndex()))).toList();
        var page = all.stream().skip((long) (pageNo - 1) * pageSize).limit(pageSize).toList();
        return R.ok(new PageData<>(page, all.size(), pageNo, pageSize));
    }

    @GetMapping("/api/front/parallax/stories")
    @Transactional(readOnly = true)
    public R<List<?>> parallaxStories() {
        return R.ok(parallaxStoryRepo.findAll().stream()
                .sorted(Comparator.comparing(s -> s.getSortIndex() == null ? 0 : s.getSortIndex())).toList());
    }

    @GetMapping("/api/front/tools/landing")
    @Transactional(readOnly = true)
    public R<List<?>> toolsLanding() {
        return R.ok(toolSiteRepo.findAll().stream()
                .sorted(Comparator.comparing(t -> t.getId()))
                .map(Dtos::toolOf).toList());
    }

    @GetMapping("/api/music")
    @Transactional(readOnly = true)
    public R<PageData<?>> music(@RequestParam(defaultValue = "1") int pageNo,
                                @RequestParam(defaultValue = "10") int pageSize) {
        var all = musicRepo.findAll();
        var page = all.stream().skip((long) (pageNo - 1) * pageSize).limit(pageSize).toList();
        return R.ok(new PageData<>(page, all.size(), pageNo, pageSize));
    }
}
