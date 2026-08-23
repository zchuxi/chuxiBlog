package com.chuxi.web;

import com.chuxi.common.R;
import com.chuxi.service.BangumiSubjectService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端番剧导入工具（/api/admin/** 受 AdminAuthInterceptor 保护）：
 *  - 搜索：bgm.tv 搜索走后端三层缓存（内存 → 磁盘 → 直连兜底），无代理也能搜已缓存的关键词；
 *  - 同步收藏：后端代理 bgm.tv（带用户 access token，个人实时数据，不缓存），服务器可达时浏览器无需代理。
 */
@RestController
@RequestMapping("/api/admin/bangumi")
public class AdminBangumiController {

    private final BangumiSubjectService bangumiSubjectService;

    public AdminBangumiController(BangumiSubjectService bangumiSubjectService) {
        this.bangumiSubjectService = bangumiSubjectService;
    }

    /** 搜索番剧条目；无缓存且拉取失败时返回业务失败（前端可降级浏览器直连） */
    @GetMapping("/search")
    public R<JsonNode> search(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return R.fail("请输入搜索关键词");
        }
        JsonNode data = bangumiSubjectService.search(keyword);
        if (data == null) {
            return R.fail("连接 Bangumi 失败，请稍后再试");
        }
        return R.ok(data);
    }

    /** 同步个人收藏：body = { token }，返回 { username, nickname, items } */
    @PostMapping("/sync-collections")
    public R<Map<String, Object>> syncCollections(@RequestBody Map<String, String> body) {
        String token = body == null ? null : body.get("token");
        if (token == null || token.trim().isEmpty()) {
            return R.fail("缺少 Bangumi 访问令牌");
        }
        Map<String, Object> result = bangumiSubjectService.syncCollections(token);
        if (result == null) {
            return R.fail("同步失败：无法连接 Bangumi 或令牌无效");
        }
        return R.ok(result);
    }
}
