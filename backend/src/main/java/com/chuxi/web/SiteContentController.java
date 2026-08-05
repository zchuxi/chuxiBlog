package com.chuxi.web;

import com.chuxi.common.ClientIpResolver;
import com.chuxi.common.R;
import com.chuxi.common.RateLimiter;
import com.chuxi.entity.SiteContent;
import com.chuxi.repo.SiteContentRepo;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 站点文案（site-content）与站点浏览量（site-views） */
@RestController
public class SiteContentController {

    static final String VIEWS_KEY = "site-views";

    /** 敏感 key 黑名单：禁止通过管理端接口直接写入 */
    private static final Set<String> PROTECTED_KEYS = Set.of("admin-password", "visitor-secret");

    /** 公开可读的文案 key 白名单：其余（如 admin-password）一律不对外暴露 */
    private static final java.util.Set<String> PUBLIC_KEYS =
            java.util.Set.of("home-landing", "about", "archive-hero", "background-gallery", VIEWS_KEY,
                    "site-settings", "nav-menu", "appearance-settings",
                    "timeline-hero", "treehole-config", "parallax-config",
                    "bangumi-hero", "calendar-hero", "tool-hero");

    private final SiteContentRepo repo;
    private final EntityManager entityManager;
    private final ClientIpResolver clientIpResolver;

    public SiteContentController(SiteContentRepo repo, EntityManager entityManager,
                                 ClientIpResolver clientIpResolver) {
        this.repo = repo;
        this.entityManager = entityManager;
        this.clientIpResolver = clientIpResolver;
    }

    /** 前台读取文案，无记录返回 code 1 */
    @GetMapping("/api/front/site-content/{key}")
    @Transactional(readOnly = true)
    @Cacheable(value = "siteContent", key = "#key")
    public R<SiteContent> front(@PathVariable String key) {
        // 非白名单 key 与不存在返回同样的响应，避免通过差异探测内部数据
        if (!PUBLIC_KEYS.contains(key)) return R.fail("内容不存在");
        SiteContent content = repo.findByContentKey(key).orElse(null);
        return content != null ? R.ok(content) : R.fail("内容不存在");
    }

    /** 管理端：全部文案列表（过滤敏感 key） */
    @GetMapping("/api/admin/site-content")
    @Transactional(readOnly = true)
    public R<List<SiteContent>> list() {
        return R.ok(repo.findAll().stream()
                .filter(e -> !PROTECTED_KEYS.contains(e.getContentKey()))
                .toList());
    }

    /** 管理端：按 key upsert，body {"contentJson":"..."} */
    @PutMapping("/api/admin/site-content/{key}")
    @Transactional
    @CacheEvict(value = "siteContent", key = "#key")
    public R<SiteContent> save(@PathVariable String key, @RequestBody Map<String, Object> body) {
        if (PROTECTED_KEYS.contains(key)) {
            return R.fail("受保护的配置项，不可直接修改");
        }
        Object json = body.get("contentJson");
        if (json == null) return R.fail("contentJson 不能为空");
        SiteContent sc = repo.findByContentKey(key).orElseGet(() -> {
            SiteContent n = new SiteContent();
            n.setContentKey(key);
            return n;
        });
        sc.setContentJson(String.valueOf(json));
        sc.setUpdatedAt(LocalDateTime.now());
        SiteContent saved = repo.save(sc);
        return R.ok(saved);
    }

    /** 前台读取站点浏览量 */
    @GetMapping("/api/front/views")
    @Transactional(readOnly = true)
    @Cacheable(value = "siteViews")
    public R<Map<String, Object>> views() {
        return R.ok(Map.of("views", readViews(repo)));
    }

    /** 前台浏览量 +1（同一 IP 每小时最多生效 1 次，超限静默返回当前计数） */
    @PostMapping("/api/front/views/bump")
    @Transactional
    @CacheEvict(value = "siteViews")
    public R<Map<String, Object>> bump(HttpServletRequest request) {
        String ip = clientIpResolver.resolve(request);
        if (!RateLimiter.tryAcquire("bump:" + ip, 3600, 1)) {
            return R.ok(Map.of("views", readViews(repo)));
        }
        // 确保记录存在
        SiteContent sc = repo.findByContentKey(VIEWS_KEY).orElseGet(() -> {
            SiteContent n = new SiteContent();
            n.setContentKey(VIEWS_KEY);
            n.setViews(0L);
            return repo.save(n);
        });
        // 原子 +1（native UPDATE 绕过 JPA 缓存，需 refresh 同步）
        repo.bumpViews(VIEWS_KEY);
        entityManager.refresh(sc);
        long views = sc.getViews() != null ? sc.getViews() : 0L;
        return R.ok(Map.of("views", views));
    }

    /** 读取 site-views 计数，无记录返回 0 */
    static long readViews(SiteContentRepo repo) {
        return repo.findByContentKey(VIEWS_KEY)
                .map(sc -> sc.getViews() != null ? sc.getViews() : 0L)
                .orElse(0L);
    }

}
