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
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
                        prev = articleRepo.findFirstByStatusNotAndIdLessThanOrderByIdDesc("草稿", a.getId())
                                .map(p -> {
                                    Map<String, Object> m = new LinkedHashMap<>();
                                    m.put("id", p.getId());
                                    m.put("title", p.getTitle());
                                    return m;
                                })
                                .orElse(null);
                        next = articleRepo.findFirstByStatusNotAndIdGreaterThanOrderByIdAsc("草稿", a.getId())
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
    public R<List<Dtos.CommentItem>> comments(@PathVariable Long id,
                                              @RequestHeader(value = "X-Visitor-Id", required = false) String visitorId) {
        List<Comment> comments = commentRepo.findByArticleIdAndApprovedTrueOrderByCreatedAtDesc(id);
        if (comments.isEmpty()) return R.ok(List.of());
        List<Long> commentIds = comments.stream().map(Comment::getId).toList();
        java.util.Set<Long> likedIds = VisitorIds.isValid(visitorId)
                ? new java.util.HashSet<>(commentLikeRepo.findCommentIdsByVisitorIdAndCommentIdIn(visitorId, commentIds))
                : java.util.Set.of();
        return R.ok(comments.stream().map(c -> Dtos.CommentItem.of(c, likedIds.contains(c.getId()))).toList());
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
                                           @RequestHeader(value = "X-Visitor-Id", required = false) String visitorId) {
        if (!VisitorIds.isValid(visitorId)) return R.fail("访客标识无效");
        return commentRepo.findByIdForUpdate(commentId).filter(c -> Boolean.TRUE.equals(c.getApproved())).map(c -> {
            boolean newLiked;
            if (commentLikeRepo.findByCommentIdAndVisitorId(commentId, visitorId).isPresent()) {
                commentLikeRepo.deleteByCommentIdAndVisitorId(commentId, visitorId);
                newLiked = false;
            } else {
                CommentLike like = new CommentLike();
                like.setCommentId(commentId);
                like.setVisitorId(visitorId);
                like.setCreatedAt(LocalDateTime.now());
                commentLikeRepo.save(like);
                newLiked = true;
            }
            int delta = newLiked ? 1 : -1;
            int newLikeCount = Math.max(0, (c.getLikeCount() == null ? 0 : c.getLikeCount()) + delta);
            c.setLikeCount(newLikeCount);
            commentRepo.save(c);
            return R.ok(Dtos.CommentItem.of(c, newLiked));
        }).orElseGet(() -> R.fail("评论不存在"));
    }

}
