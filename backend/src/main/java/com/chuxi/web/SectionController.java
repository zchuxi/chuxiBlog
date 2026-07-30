package com.chuxi.web;

import com.chuxi.common.ClientIpResolver;
import com.chuxi.common.InputSanitizer;
import com.chuxi.common.PageData;
import com.chuxi.common.R;
import com.chuxi.common.RateLimiter;
import com.chuxi.entity.Article;
import com.chuxi.entity.Barrage;
import com.chuxi.entity.FriendLink;
import com.chuxi.repo.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    private final FriendLinkRepo friendLinkRepo;
    private final ClientIpResolver clientIpResolver;

    public SectionController(TimelineCarouselRepo timelineCarouselRepo, TimelineEventRepo timelineEventRepo,
                             ArticleRepo articleRepo, ArchiveCategoryRepo archiveCategoryRepo,
                             BarrageRepo barrageRepo, CalledTextRepo calledTextRepo,
                             ParallaxStoryRepo parallaxStoryRepo, ToolSiteRepo toolSiteRepo,
                             MusicRepo musicRepo, FriendLinkRepo friendLinkRepo,
                             ClientIpResolver clientIpResolver) {
        this.timelineCarouselRepo = timelineCarouselRepo;
        this.timelineEventRepo = timelineEventRepo;
        this.articleRepo = articleRepo;
        this.archiveCategoryRepo = archiveCategoryRepo;
        this.barrageRepo = barrageRepo;
        this.calledTextRepo = calledTextRepo;
        this.parallaxStoryRepo = parallaxStoryRepo;
        this.toolSiteRepo = toolSiteRepo;
        this.musicRepo = musicRepo;
        this.friendLinkRepo = friendLinkRepo;
        this.clientIpResolver = clientIpResolver;
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
        var pageable = PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        var result = barrageRepo.findByApprovedTrue(pageable);
        return R.ok(new PageData<>(result.getContent(), result.getTotalElements(), pageNo, pageSize));
    }

    @PostMapping("/api/front/tree-hole/barrages")
    @Transactional
    public R<Barrage> addBarrage(@Valid @RequestBody BarrageRequest req,
                                 HttpServletRequest request) {
        String ip = clientIpResolver.resolve(request);
        if (!RateLimiter.tryAcquire(ip)) {
            return R.fail("提交过于频繁，请稍后再试");
        }
        String content = InputSanitizer.sanitize(req.getContent());
        String nickname = InputSanitizer.sanitize(req.getNickname());
        Barrage b = new Barrage();
        b.setUserId(1L);
        b.setNickname(nickname == null || nickname.isEmpty() ? "树友-0001" : nickname);
        b.setMood(req.getMood() != null ? req.getMood() : "轻声");
        b.setContent(content);
        b.setLikeCount(0);
        b.setLiked(false);
        b.setApproved(true);
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
        var pageable = PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "sortIndex"));
        var result = calledTextRepo.findAll(pageable);
        return R.ok(new PageData<>(result.getContent(), result.getTotalElements(), pageNo, pageSize));
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
        var pageable = PageRequest.of(pageNo - 1, pageSize);
        var result = musicRepo.findAll(pageable);
        return R.ok(new PageData<>(result.getContent(), result.getTotalElements(), pageNo, pageSize));
    }

    @GetMapping("/api/front/friend-links")
    @Transactional(readOnly = true)
    public List<FriendLink> friendLinks() {
        return friendLinkRepo.findByVisibleTrueOrderBySortIndexAsc();
    }

}
