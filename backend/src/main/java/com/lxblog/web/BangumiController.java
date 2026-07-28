package com.lxblog.web;

import com.lxblog.common.R;
import com.lxblog.entity.BangumiRecord;
import com.lxblog.repo.BangumiRecordRepo;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 前台番剧记录列表 */
@RestController
public class BangumiController {

    private final BangumiRecordRepo bangumiRecordRepo;

    public BangumiController(BangumiRecordRepo bangumiRecordRepo) {
        this.bangumiRecordRepo = bangumiRecordRepo;
    }

    @GetMapping("/api/front/bangumi")
    @Transactional(readOnly = true)
    public R<List<Map<String, Object>>> list() {
        return R.ok(bangumiRecordRepo.findAll().stream()
                // 后台关掉展示开关的记录仅在管理端可见
                .filter(b -> b.getVisible() == null || b.getVisible())
                .sorted(Comparator
                        .comparing((BangumiRecord b) -> b.getSortIndex() == null ? Integer.MAX_VALUE : b.getSortIndex())
                        .thenComparing(BangumiRecord::getUpdatedAt,
                                Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder())))
                .map(this::toOut).toList());
    }

    @GetMapping("/api/front/bangumi/{id}")
    @Transactional(readOnly = true)
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return bangumiRecordRepo.findById(id)
                .map(b -> R.ok(toOut(b)))
                .orElseGet(() -> R.fail("记录不存在"));
    }

    /** tags CSV -> 数组，其余字段原样输出 */
    private Map<String, Object> toOut(BangumiRecord b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", b.getId());
        m.put("subjectId", b.getSubjectId());
        m.put("name", b.getName());
        m.put("nameCn", b.getNameCn());
        m.put("coverUrl", b.getCoverUrl() == null ? "" : b.getCoverUrl());
        m.put("totalEps", b.getTotalEps());
        m.put("watchedEps", b.getWatchedEps());
        m.put("status", b.getStatus());
        m.put("rating", b.getRating());
        m.put("score", b.getScore());
        m.put("airDate", b.getAirDate());
        m.put("platform", b.getPlatform());
        m.put("rank", b.getRank());
        m.put("ratingTotal", b.getRatingTotal());
        m.put("summary", b.getSummary());
        m.put("tags", Dtos.splitTags(b.getTags()));
        m.put("category", b.getCategory() == null ? "" : b.getCategory());
        m.put("sortIndex", b.getSortIndex());
        m.put("createdAt", b.getCreatedAt());
        m.put("updatedAt", b.getUpdatedAt());
        return m;
    }
}
