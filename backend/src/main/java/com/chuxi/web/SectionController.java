package com.chuxi.web;

import com.chuxi.common.ClientIpResolver;
import com.chuxi.common.InputSanitizer;
import com.chuxi.common.PageData;
import com.chuxi.common.R;
import com.chuxi.common.RateLimiter;
import com.chuxi.common.VisitorIds;
import com.chuxi.entity.Barrage;
import com.chuxi.entity.FriendLink;
import com.chuxi.repo.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
        data.put("carousels", timelineCarouselRepo.findAllByOrderByIdDesc());
        data.put("timelines", timelineEventRepo.findAllByOrderByIdAsc());
        return R.ok(data);
    }

    @GetMapping("/api/front/archive/landing")
    @Transactional(readOnly = true)
    public R<Map<String, Object>> archiveLanding() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("entries", articleRepo.findAllPublishedOrderByPublishedAtDesc().stream()
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
        if (pageNo < 1 || pageSize < 1 || pageSize > 100) return R.fail("分页参数无效");
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
        String nickname = InputSanitizer.truncate(InputSanitizer.sanitize(req.getNickname()), 20);
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

    /** 匿名访客身份签发：返回服务端 HMAC 签名的 visitor token（IP 限流，防止批量伪造匿名主体） */
    @GetMapping("/api/front/visitor/token")
    public R<Map<String, String>> visitorToken(HttpServletRequest request) {
        String ip = clientIpResolver.resolve(request);
        if (!RateLimiter.tryAcquire("visitor:" + ip, 60, 30)) {
            return R.fail("请求过于频繁，请稍后再试");
        }
        String rawId = VisitorIds.newRawId();
        return R.ok(Map.of("token", VisitorIds.issue(rawId)));
    }

    @PostMapping("/api/front/tree-hole/barrages/{id}/likes")
    @Transactional
    public R<Barrage> likeBarrage(@PathVariable Long id,
                                  @RequestHeader(value = "X-Visitor-Id", required = false) String visitorId,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
        String ip = clientIpResolver.resolve(request);
        if (!RateLimiter.tryAcquire("barrageLike:" + ip, 60, 10)) {
            return R.fail("操作过于频繁，请稍后再试");
        }
        // fail-closed：匿名身份必须是服务端签发的合法 token，客户端无法自行构造
        if (VisitorIds.resolve(visitorId) == null) {
            response.setHeader("X-Visitor-Token", VisitorIds.issue(VisitorIds.newRawId()));
            return R.fail("访客标识无效，请刷新后重试");
        }
        return barrageRepo.findById(id).map(b -> {
            boolean liked = Boolean.TRUE.equals(b.getLiked());
            boolean newLiked = !liked;
            int newLikeCount = Math.max(0, (b.getLikeCount() == null ? 0 : b.getLikeCount()) + (liked ? -1 : 1));
            barrageRepo.updateLike(id, newLiked, newLikeCount);
            b.setLiked(newLiked);
            b.setLikeCount(newLikeCount);
            return R.ok(b);
        }).orElseGet(() -> R.fail("弹幕不存在"));
    }

    @GetMapping("/api/front/tree-hole/called-texts")
    @Transactional(readOnly = true)
    public R<PageData<?>> calledTexts(@RequestParam(defaultValue = "1") int pageNo,
                                      @RequestParam(defaultValue = "50") int pageSize) {
        if (pageNo < 1 || pageSize < 1 || pageSize > 100) return R.fail("分页参数无效");
        var pageable = PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "sortIndex"));
        var result = calledTextRepo.findAll(pageable);
        return R.ok(new PageData<>(result.getContent(), result.getTotalElements(), pageNo, pageSize));
    }

    @GetMapping("/api/front/parallax/stories")
    @Transactional(readOnly = true)
    public R<List<?>> parallaxStories() {
        return R.ok(parallaxStoryRepo.findAllByOrderBySortIndexAsc());
    }

    @GetMapping("/api/front/tools/landing")
    @Transactional(readOnly = true)
    public R<List<?>> toolsLanding() {
        return R.ok(toolSiteRepo.findAllByOrderByIdAsc().stream()
                .map(Dtos::toolOf).toList());
    }

    @GetMapping("/api/music")
    @Transactional(readOnly = true)
    public R<PageData<?>> music(@RequestParam(defaultValue = "1") int pageNo,
                                @RequestParam(defaultValue = "10") int pageSize) {
        if (pageNo < 1 || pageSize < 1 || pageSize > 100) return R.fail("分页参数无效");
        var pageable = PageRequest.of(pageNo - 1, pageSize);
        var result = musicRepo.findAll(pageable);
        return R.ok(new PageData<>(result.getContent(), result.getTotalElements(), pageNo, pageSize));
    }

    @GetMapping("/api/front/friend-links")
    @Transactional(readOnly = true)
    public R<List<FriendLink>> friendLinks() {
        return R.ok(friendLinkRepo.findByVisibleTrueOrderBySortIndexAsc());
    }

}
