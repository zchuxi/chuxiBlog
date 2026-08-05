package com.chuxi.web;

import com.chuxi.common.ClientIpResolver;
import com.chuxi.common.InputSanitizer;
import com.chuxi.common.PageData;
import com.chuxi.common.R;
import com.chuxi.common.RateLimiter;
import com.chuxi.common.VisitorIds;
import com.chuxi.entity.Article;
import com.chuxi.entity.Comment;
import com.chuxi.entity.CommentLike;
import com.chuxi.repo.CommentLikeRepo;
import com.chuxi.repo.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/front/articles")
public class ArticleController {

    private final ArticleRepo articleRepo;
    private final CommentRepo commentRepo;
    private final CommentLikeRepo commentLikeRepo;
    private final ClientIpResolver clientIpResolver;

    public ArticleController(ArticleRepo articleRepo, CommentRepo commentRepo, CommentLikeRepo commentLikeRepo,
                             ClientIpResolver clientIpResolver) {
        this.articleRepo = articleRepo;
        this.commentRepo = commentRepo;
        this.commentLikeRepo = commentLikeRepo;
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping("/search")
    @Transactional(readOnly = true)
    public R<PageData<Dtos.ArticleItem>> search(@RequestParam(defaultValue = "") String keyword,
                                                @RequestParam(defaultValue = "1") int pageNo,
                                                @RequestParam(defaultValue = "6") int pageSize) {
        if (pageNo < 1 || pageSize < 1 || pageSize > 50) {
            return R.fail("分页参数无效");
        }
        var pageable = PageRequest.of(pageNo - 1, pageSize);
        String kw = keyword.trim();
        var result = kw.isEmpty()
                ? articleRepo.findAllPublishedByUpdatedAtDesc(pageable)
                : articleRepo.searchPublished(kw.toLowerCase(), pageable);
        var items = result.getContent().stream().map(Dtos.ArticleItem::of).toList();
        return R.ok(new PageData<>(items, result.getTotalElements(), pageNo, pageSize));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return articleRepo.findById(id)
                // 草稿与不存在返回同样的响应，避免通过差异探测草稿 id
                .filter(a -> !"草稿".equals(a.getStatus()))
                .map(a -> {
                    Map<String, Object> prev = null;
                    Map<String, Object> next = null;
                    try {
                        prev = articleRepo.findPrevPublished(a.getId(), PageRequest.of(0, 1)).stream().findFirst()
                                .map(p -> {
                                    Map<String, Object> m = new LinkedHashMap<>();
                                    m.put("id", p.getId());
                                    m.put("title", p.getTitle());
                                    return m;
                                })
                                .orElse(null);
                        next = articleRepo.findNextPublished(a.getId(), PageRequest.of(0, 1)).stream().findFirst()
                                .map(n -> {
                                    Map<String, Object> m = new LinkedHashMap<>();
                                    m.put("id", n.getId());
                                    m.put("title", n.getTitle());
                                    return m;
                                })
                                .orElse(null);
                    } catch (Exception ex) {
                        // 前后篇查询失败不阻断详情：降级为 null，正文仍可正常返回
                        org.slf4j.LoggerFactory.getLogger(getClass()).warn("[文章] 前后篇查询失败 id={}", a.getId(), ex);
                    }
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("article", Dtos.ArticleDetail.of(a));
                    body.put("prev", prev);
                    body.put("next", next);
                    return R.ok(body);
                })
                .orElseGet(() -> R.fail("文章不存在"));
    }

    @GetMapping("/{id}/comments")
    @Transactional(readOnly = true)
    public R<PageData<Dtos.CommentItem>> comments(@PathVariable Long id,
                                                  @RequestParam(defaultValue = "1") int pageNo,
                                                  @RequestParam(defaultValue = "20") int pageSize,
                                                  @RequestHeader(value = "X-Visitor-Id", required = false) String visitorId) {
        // SEC-002：强制分页并限制最大页大小，评论集合无界增长时单次响应仍固定有界
        if (pageNo < 1 || pageSize < 1 || pageSize > 50) return R.fail("分页参数无效");
        var pageable = PageRequest.of(pageNo - 1, pageSize);
        var page = commentRepo.findByArticleIdAndApprovedTrueOrderByCreatedAtDesc(id, pageable);
        var comments = page.getContent();
        if (comments.isEmpty()) return R.ok(new PageData<>(List.of(), page.getTotalElements(), pageNo, pageSize));
        List<Long> commentIds = comments.stream().map(Comment::getId).toList();
        // 点赞关系只查询当前页的评论 ID；visitor token 验签后剥离签名，取 rawId 查询
        String rawId = VisitorIds.resolve(visitorId);
        Set<Long> likedIds = rawId != null
                ? new HashSet<>(commentLikeRepo.findCommentIdsByVisitorIdAndCommentIdIn(rawId, commentIds))
                : Set.of();
        var items = comments.stream().map(c -> Dtos.CommentItem.of(c, likedIds.contains(c.getId()))).toList();
        return R.ok(new PageData<>(items, page.getTotalElements(), pageNo, pageSize));
    }

    @PostMapping("/{id}/comments")
    @Transactional
    public R<Comment> addComment(@PathVariable Long id,
                                 @Valid @RequestBody CommentRequest req,
                                 HttpServletRequest request) {
        boolean published = articleRepo.findById(id)
                .map(article -> !"草稿".equals(article.getStatus()))
                .orElse(false);
        if (!published) {
            return R.fail("文章不存在");
        }
        String ip = clientIpResolver.resolve(request);
        if (!RateLimiter.tryAcquire(ip)) {
            return R.fail("提交过于频繁，请稍后再试");
        }
        String content = InputSanitizer.sanitize(req.getContent());
        String nickname = InputSanitizer.truncate(InputSanitizer.sanitize(req.getNickname()), 20);
        Comment c = new Comment();
        c.setArticleId(id);
        c.setNickname(nickname.isEmpty() ? "访客" : nickname);
        c.setContent(content);
        c.setLikeCount(0);
        c.setLiked(false);
        c.setApproved(true);
        c.setCreatedAt(LocalDateTime.now());
        return R.ok(commentRepo.save(c));
    }

    @PostMapping("/comments/{commentId}/likes")
    @Transactional
    public R<Dtos.CommentItem> likeComment(@PathVariable Long commentId,
                                           @RequestHeader(value = "X-Visitor-Id", required = false) String visitorId,
                                           HttpServletRequest request,
                                           HttpServletResponse response) {
        // SEC-001：按 IP 限流，防止同一来源无限翻转/放大点赞
        String ip = clientIpResolver.resolve(request);
        if (!RateLimiter.tryAcquire("commentLike:" + ip, 60, 10)) {
            return R.fail("操作过于频繁，请稍后再试");
        }
        // fail-closed：匿名身份必须是服务端 HMAC 签发的合法 token，客户端无法自行构造
        String rawId = VisitorIds.resolve(visitorId);
        if (rawId == null) {
            response.setHeader("X-Visitor-Token", VisitorIds.issue(VisitorIds.newRawId()));
            return R.fail("访客标识无效，请刷新后重试");
        }
        return commentRepo.findByIdForUpdate(commentId).filter(c -> Boolean.TRUE.equals(c.getApproved())).map(c -> {
            boolean newLiked;
            int delta;
            if (commentLikeRepo.findByCommentIdAndVisitorId(commentId, rawId).isPresent()) {
                long deleted = commentLikeRepo.deleteByCommentIdAndVisitorId(commentId, rawId);
                newLiked = false;
                // 仅在确实删除到记录时才扣减计数；记录已被其他路径删除时不再扣，避免计数漂移
                delta = deleted > 0 ? -1 : 0;
            } else {
                CommentLike like = new CommentLike();
                like.setCommentId(commentId);
                like.setVisitorId(rawId);
                like.setCreatedAt(LocalDateTime.now());
                commentLikeRepo.save(like);
                newLiked = true;
                delta = 1;
            }
            int newLikeCount = Math.max(0, (c.getLikeCount() == null ? 0 : c.getLikeCount()) + delta);
            c.setLikeCount(newLikeCount);
            commentRepo.save(c);
            return R.ok(Dtos.CommentItem.of(c, newLiked));
        }).orElseGet(() -> R.fail("评论不存在"));
    }

}
