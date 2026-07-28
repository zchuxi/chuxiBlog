package com.lxblog.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lxblog.common.R;
import com.lxblog.entity.SiteContent;
import com.lxblog.repo.SiteContentRepo;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 站点文案（site-content）与站点浏览量（site-views） */
@RestController
public class SiteContentController {

    static final String VIEWS_KEY = "site-views";

    /** 公开可读的文案 key 白名单：其余（如 admin-password）一律不对外暴露 */
    private static final java.util.Set<String> PUBLIC_KEYS =
            java.util.Set.of("home-landing", "about", "archive-hero", "background-gallery", VIEWS_KEY);

    private final SiteContentRepo repo;
    private final ObjectMapper mapper;

    public SiteContentController(SiteContentRepo repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    /** 前台读取文案，无记录返回 code 1 */
    @GetMapping("/api/front/site-content/{key}")
    @Transactional(readOnly = true)
    public R<SiteContent> front(@PathVariable String key) {
        // 非白名单 key 与不存在返回同样的响应，避免通过差异探测内部数据
        if (!PUBLIC_KEYS.contains(key)) return R.fail("内容不存在");
        return repo.findByContentKey(key).map(R::ok).orElseGet(() -> R.fail("内容不存在"));
    }

    /** 管理端：全部文案列表 */
    @GetMapping("/api/admin/site-content")
    @Transactional(readOnly = true)
    public R<List<SiteContent>> list() {
        return R.ok(repo.findAll());
    }

    /** 管理端：按 key upsert，body {"contentJson":"..."} */
    @PutMapping("/api/admin/site-content/{key}")
    @Transactional
    public R<SiteContent> save(@PathVariable String key, @RequestBody Map<String, Object> body) {
        Object json = body.get("contentJson");
        if (json == null) return R.fail("contentJson 不能为空");
        SiteContent sc = repo.findByContentKey(key).orElseGet(() -> {
            SiteContent n = new SiteContent();
            n.setContentKey(key);
            return n;
        });
        sc.setContentJson(String.valueOf(json));
        sc.setUpdatedAt(LocalDateTime.now());
        return R.ok(repo.save(sc));
    }

    /** 前台读取站点浏览量 */
    @GetMapping("/api/front/views")
    @Transactional(readOnly = true)
    public R<Map<String, Object>> views() {
        return R.ok(Map.of("views", readViews(repo, mapper)));
    }

    /** 前台浏览量 +1 */
    @PostMapping("/api/front/views/bump")
    @Transactional
    public R<Map<String, Object>> bump() {
        SiteContent sc = repo.findByContentKey(VIEWS_KEY).orElseGet(() -> {
            SiteContent n = new SiteContent();
            n.setContentKey(VIEWS_KEY);
            return n;
        });
        long views = parseViews(mapper, sc.getContentJson()) + 1;
        sc.setContentJson("{\"views\":" + views + "}");
        sc.setUpdatedAt(LocalDateTime.now());
        repo.save(sc);
        return R.ok(Map.of("views", views));
    }

    /** 读取 site-views 计数，无记录/解析失败返回 0 */
    static long readViews(SiteContentRepo repo, ObjectMapper mapper) {
        return repo.findByContentKey(VIEWS_KEY)
                .map(sc -> parseViews(mapper, sc.getContentJson()))
                .orElse(0L);
    }

    private static long parseViews(ObjectMapper mapper, String json) {
        if (json == null || json.isBlank()) return 0L;
        try {
            return mapper.readTree(json).path("views").asLong(0L);
        } catch (Exception e) {
            return 0L;
        }
    }
}
