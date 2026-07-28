package com.lxblog.web;

import com.lxblog.common.PageData;
import com.lxblog.common.R;
import com.lxblog.entity.Article;
import com.lxblog.entity.Comment;
import com.lxblog.repo.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/front/articles")
public class ArticleController {

    private final ArticleRepo articleRepo;
    private final CommentRepo commentRepo;

    public ArticleController(ArticleRepo articleRepo, CommentRepo commentRepo) {
        this.articleRepo = articleRepo;
        this.commentRepo = commentRepo;
    }

    @GetMapping("/search")
    @Transactional(readOnly = true)
    public R<PageData<Dtos.ArticleItem>> search(@RequestParam(defaultValue = "") String keyword,
                                                @RequestParam(defaultValue = "1") int pageNo,
                                                @RequestParam(defaultValue = "6") int pageSize) {
        String kw = keyword.trim().toLowerCase();
        List<Article> hits = articleRepo.findAll().stream()
                .filter(a -> !"草稿".equals(a.getStatus()))
                .filter(a -> kw.isEmpty()
                        || (a.getTitle() != null && a.getTitle().toLowerCase().contains(kw))
                        || (a.getSummary() != null && a.getSummary().toLowerCase().contains(kw))
                        || (a.getTags() != null && a.getTags().toLowerCase().contains(kw)))
                .sorted(Comparator.comparing(Article::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        var page = hits.stream().skip((long) (pageNo - 1) * pageSize).limit(pageSize)
                .map(Dtos.ArticleItem::of).toList();
        return R.ok(new PageData<>(page, hits.size(), pageNo, pageSize));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public R<Dtos.ArticleDetail> detail(@PathVariable Long id) {
        return articleRepo.findById(id)
                // 草稿与不存在返回同样的响应，避免通过差异探测草稿 id
                .filter(a -> !"草稿".equals(a.getStatus()))
                .map(a -> R.ok(Dtos.ArticleDetail.of(a)))
                .orElseGet(() -> R.fail("文章不存在"));
    }

    @GetMapping("/{id}/comments")
    @Transactional(readOnly = true)
    public R<List<Comment>> comments(@PathVariable Long id) {
        return R.ok(commentRepo.findByArticleIdOrderByCreatedAtDesc(id));
    }

    @PostMapping("/{id}/comments")
    @Transactional
    public R<Comment> addComment(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String content = body.getOrDefault("content", "").trim();
        if (content.isEmpty()) return R.fail("内容不能为空");
        Comment c = new Comment();
        c.setArticleId(id);
        c.setNickname(body.getOrDefault("nickname", "访客"));
        c.setContent(content);
        c.setLikeCount(0);
        c.setLiked(false);
        c.setCreatedAt(LocalDateTime.now());
        return R.ok(commentRepo.save(c));
    }

    @PostMapping("/comments/{commentId}/likes")
    @Transactional
    public R<Comment> likeComment(@PathVariable Long commentId) {
        return commentRepo.findById(commentId).map(c -> {
            boolean liked = Boolean.TRUE.equals(c.getLiked());
            c.setLiked(!liked);
            c.setLikeCount(Math.max(0, (c.getLikeCount() == null ? 0 : c.getLikeCount()) + (liked ? -1 : 1)));
            return R.ok(commentRepo.save(c));
        }).orElseGet(() -> R.fail("评论不存在"));
    }
}
