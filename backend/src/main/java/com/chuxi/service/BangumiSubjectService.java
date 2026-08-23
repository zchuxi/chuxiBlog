package com.chuxi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bangumi 番剧在线数据（条目 / 剧集 / 角色 / 搜索）三层缓存 + 收藏同步代理：
 *  1) 内存缓存（6h TTL）—— 最快，零网络
 *  2) 磁盘缓存 data/bangumi-bgm/{name}.json —— api.bgm.tv 不可达时兜底，重启后仍生效
 *  3) 直连 api.bgm.tv —— 仅在前两层都缺失时才尝试（connectTimeout 3s / 请求 5s）
 *
 * <p>与 BangumiCalendarService 同一套模式：服务器无法直连 api.bgm.tv（GFW/IP 级拦截）时，
 * 磁盘缓存由本机脚本 scripts/fetch_and_upload_bangumi_subjects.py 走代理拉取后 SFTP 上传
 * （路径与服务运行目录下 data/ 相对路径一致，服务器为 /opt/chuxi/data/bangumi-bgm/）。
 *
 * <p>kind 取值：subject（条目详情）、episodes（剧集列表）、characters（角色列表）；
 * 搜索按关键词缓存（文件名 search-{md5(keyword)}.json）；收藏同步为个人实时数据，只代理不缓存。
 */
@Service
public class BangumiSubjectService {

    private static final Logger log = LoggerFactory.getLogger(BangumiSubjectService.class);

    private static final String API_BASE = "https://api.bgm.tv";
    private static final long CACHE_TTL = 6 * 60 * 60 * 1000L; // 6 小时
    private static final Set<String> KINDS = Set.of("subject", "episodes", "characters");

    /** 磁盘缓存目录：相对服务运行目录（服务器 /opt/chuxi -> /opt/chuxi/data/bangumi-bgm/） */
    private static final Path CACHE_DIR = Paths.get("data", "bangumi-bgm");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .proxy(ProxySelector.getDefault())
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    /** 可选 bgm.tv access token（提高限流配额），由 app.bangumi.token 注入 */
    private final String token;

    private final Map<String, CacheEntry> memoryCache = new ConcurrentHashMap<>();

    public BangumiSubjectService(@Value("${app.bangumi.token:}") String token) {
        this.token = token == null ? "" : token.trim();
    }

    @PostConstruct
    public void initDiskCacheDir() {
        try {
            Files.createDirectories(CACHE_DIR);
        } catch (Exception e) {
            log.warn("Could not create bangumi-bgm cache dir: {}", e.getMessage());
        }
    }

    public boolean supports(String kind) {
        return KINDS.contains(kind);
    }

    /**
     * 获取番剧在线数据；任何情况下不抛异常。
     * 无缓存且直连失败时返回 null（调用方据此降级隐藏对应区块）。
     */
    public JsonNode fetch(String kind, long sid) {
        if (!supports(kind)) return null;
        return getWithCache(kind + "-" + sid, () -> liveFetch(kind, sid));
    }

    /**
     * 搜索番剧（v0 搜索优先，旧版接口降级）；无缓存且直连失败时返回 null。
     * 返回体为条目数组（已归一化形状与前端一致）。
     */
    public JsonNode search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return null;
        String kw = keyword.trim();
        String name = "search-" + md5Hex(kw);
        return getWithCache(name, () -> liveSearch(kw));
    }

    /**
     * 同步个人收藏：后端代理 bgm.tv（使用用户提供的 access token，实时数据不做缓存）。
     * 返回 { username, nickname, items }；令牌无效或网络失败时返回 null。
     */
    public Map<String, Object> syncCollections(String userToken) {
        String bearer = userToken == null ? "" : userToken.trim();
        if (bearer.isEmpty()) return null;
        // 1) 拿用户名
        JsonNode me = bgmGet("/v0/me", bearer);
        if (me == null) {
            log.warn("bgm /v0/me failed (token invalid or network)");
            return null;
        }
        String username = me.path("username").asText("");
        if (username.isEmpty()) return null;
        String nickname = me.path("nickname").asText(username);
        // 2) 分页拉全部动画收藏（subject_type=2）
        List<JsonNode> items = new ArrayList<>();
        int offset = 0;
        while (items.size() < 1000) {
            JsonNode page = bgmGet(
                    "/v0/users/" + URLEncoder.encode(username, StandardCharsets.UTF_8)
                            + "/collections?subject_type=2&limit=50&offset=" + offset,
                    bearer);
            if (page == null) break;
            int total = page.path("total").asInt(0);
            JsonNode arr = page.path("data");
            if (!arr.isArray()) break;
            for (JsonNode it : arr) items.add(it);
            if (items.size() >= total || arr.size() == 0) break;
            offset += arr.size();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("username", username);
        out.put("nickname", nickname);
        out.put("items", items);
        return out;
    }

    /* ---------- 缓存通用流程 ---------- */

    private JsonNode getWithCache(String name, LiveSupplier live) {
        long now = System.currentTimeMillis();
        // 1) 内存缓存
        CacheEntry e = memoryCache.get(name);
        if (e != null && now - e.time < CACHE_TTL) {
            JsonNode n = parseSafe(e.body);
            if (n != null) return n;
            log.warn("Bangumi {} in-memory cache invalid, falling through", name);
        }
        // 2) 磁盘缓存（api.bgm.tv 被墙时由本机脚本预置）
        String disk = readDiskCache(name);
        if (disk != null) {
            JsonNode n = parseSafe(disk);
            if (n == null) {
                log.warn("Bangumi {} disk cache invalid, falling through to live fetch", name);
            } else {
                // 解析成功才落内存缓存，避免损坏的磁盘文件挤掉内存中过期的旧好数据
                memoryCache.put(name, new CacheEntry(disk, now));
                return n;
            }
        }
        // 3) 直连兜底（仅完全无缓存时），成功后写入内存 + 磁盘缓存
        String body = live.get();
        if (body != null) {
            JsonNode n = parseSafe(body);
            if (n != null) {
                memoryCache.put(name, new CacheEntry(body, now));
                persistDiskCache(name, body);
                return n;
            }
            log.warn("Bangumi {} live response invalid (unparseable body)", name);
        }
        // 4) 最后兜底：内存残值（过期但可解析）
        if (e != null) {
            JsonNode n = parseSafe(e.body);
            if (n != null) return n;
        }
        return null;
    }

    private String liveFetch(String kind, long sid) {
        String path;
        if ("subject".equals(kind)) {
            path = "/v0/subjects/" + sid;
        } else if ("episodes".equals(kind)) {
            path = "/v0/episodes?subject_id=" + sid + "&type=0&limit=100&offset=0";
        } else if ("characters".equals(kind)) {
            path = "/v0/subjects/" + sid + "/characters";
        } else {
            return null;
        }
        return httpGet(path, null);
    }

    /** v0 搜索（POST）失败后降级旧版搜索接口（GET），返回条目数组 JSON 字符串 */
    private String liveSearch(String kw) {
        // 首选 v0 搜索接口
        try {
            ObjectNode reqBody = mapper.createObjectNode();
            reqBody.put("keyword", kw);
            ObjectNode filter = reqBody.putObject("filter");
            filter.putArray("type").add(2);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/v0/search/subjects"))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "chuxi-web/1.0 (https://www.chuxi.online)")
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(reqBody)))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonNode n = parseSafe(resp.body());
                if (n != null && n.has("data") && n.get("data").isArray()) {
                    return mapper.writeValueAsString(n.get("data"));
                }
            } else {
                log.warn("bgm v0 search returned status {}", resp.statusCode());
            }
        } catch (Exception e) {
            log.warn("bgm v0 search failed: {} (falling back to legacy)", e.getMessage());
        }
        // 降级旧版搜索接口
        try {
            String encoded = URLEncoder.encode(kw, StandardCharsets.UTF_8);
            HttpResponse<String> resp = httpClient.send(HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/search/subject/" + encoded
                            + "?type=2&responseGroup=large&max_results=10"))
                    .header("User-Agent", "chuxi-web/1.0 (https://www.chuxi.online)")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonNode n = parseSafe(resp.body());
                if (n != null && n.has("list") && n.get("list").isArray()) {
                    return mapper.writeValueAsString(n.get("list"));
                }
            } else {
                log.warn("bgm legacy search returned status {}", resp.statusCode());
            }
        } catch (Exception e) {
            log.warn("bgm legacy search failed: {}", e.getMessage());
        }
        return null;
    }

    /** GET bgm.tv 路径；带可选 Bearer token；失败返回 null */
    private String httpGet(String path, String bearer) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + path))
                    .header("User-Agent", "chuxi-web/1.0 (https://www.chuxi.online)")
                    .timeout(Duration.ofSeconds(5))
                    .GET();
            if (bearer != null && !bearer.isEmpty()) {
                builder.header("Authorization", "Bearer " + bearer);
            }
            HttpResponse<String> resp = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                return resp.body();
            }
            log.warn("bgm GET {} returned status {}", path, resp.statusCode());
        } catch (Exception e) {
            log.warn("bgm GET {} failed: {}", path, e.getMessage());
        }
        return null;
    }

    private JsonNode bgmGet(String path, String bearer) {
        String body = httpGet(path, bearer);
        return body == null ? null : parseSafe(body);
    }

    private JsonNode parseSafe(String body) {
        try {
            return mapper.readTree(body);
        } catch (Exception e) {
            log.warn("Bangumi cache parse failed: {}", e.getMessage());
            return null;
        }
    }

    private Path diskFile(String name) {
        return CACHE_DIR.resolve(name + ".json");
    }

    private void persistDiskCache(String name, String body) {
        try {
            Files.writeString(diskFile(name), body, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Failed to write Bangumi {} disk cache: {}", name, e.getMessage());
        }
    }

    private String readDiskCache(String name) {
        try {
            Path p = diskFile(name);
            if (Files.exists(p)) {
                return Files.readString(p, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.warn("Failed to read Bangumi {} disk cache: {}", name, e.getMessage());
        }
        return null;
    }

    private static String md5Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }

    private record CacheEntry(String body, long time) {
    }

    @FunctionalInterface
    private interface LiveSupplier {
        String get();
    }
}
