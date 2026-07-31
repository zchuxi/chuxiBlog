package com.chuxi.web;

import com.chuxi.common.ClientIpResolver;
import com.chuxi.common.InputSanitizer;
import com.chuxi.common.PageData;
import com.chuxi.common.R;
import com.chuxi.common.RateLimiter;
import com.chuxi.entity.Article;
import com.chuxi.entity.Comment;
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
    private final ClientIpResolver clientIpResolver;

    public ArticleController(ArticleRepo articleRepo, CommentRepo commentRepo, ClientIpResolver clientIpResolver) {
        this.articleRepo = articleRepo;
        this.commentRepo = commentRepo;
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping("/search")
    @Transactional(readOnly = true)
    public R<PageData<Dtos.ArticleItem>> search(@RequestParam(defaultValue = "") String keyword,
                                                @RequestParam(defaultValue = "1") int pageNo,
                                                @RequestParam(defaultValue = "6") int pageSize) {
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
                    Map<String, Object> prev = articleRepo.findPrevious(a.getId())
                            .map(p -> {
                                Map<String, Object> m = new LinkedHashMap<>();
                                m.put("id", p.getId());
                                m.put("title", p.getTitle());
                                return m;
                            })
                            .orElse(null);
                    Map<String, Object> next = articleRepo.findNext(a.getId())
                            .map(n -> {
                                Map<String, Object> m = new LinkedHashMap<>();
                                m.put("id", n.getId());
                                m.put("title", n.getTitle());
                                return m;
                            })
                            .orElse(null);
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
    public R<List<Comment>> comments(@PathVariable Long id) {
        return R.ok(commentRepo.findByArticleIdAndApprovedTrueOrderByCreatedAtDesc(id));
    }

    @PostMapping("/{id}/comments")
    @Transactional
    public R<Comment> addComment(@PathVariable Long id,
                                 @Valid @RequestBody CommentRequest req,
                                 HttpServletRequest request) {
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
    public R<Comment> likeComment(@PathVariable Long commentId) {
        return commentRepo.findById(commentId).map(c -> {
            boolean liked = Boolean.TRUE.equals(c.getLiked());
            boolean newLiked = !liked;
            int newLikeCount = Math.max(0, (c.getLikeCount() == null ? 0 : c.getLikeCount()) + (liked ? -1 : 1));
            commentRepo.updateLike(commentId, newLiked, newLikeCount);
            c.setLiked(newLiked);
            c.setLikeCount(newLikeCount);
            return R.ok(c);
        }).orElseGet(() -> R.fail("评论不存在"));
    }

}
