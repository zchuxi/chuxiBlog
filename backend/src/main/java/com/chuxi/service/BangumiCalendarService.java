package com.chuxi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * Bangumi 每周放送日历（三层缓存）：
 *  1) 内存缓存（30min TTL）—— 最快，零网络
 *  2) 磁盘缓存 data/bangumi-calendar.json —— api.bgm.tv 不可达时兜底，重启后仍生效
 *  3) 直连 api.bgm.tv —— 仅在前两层都缺失时才尝试（connectTimeout 3s / 请求 5s）
 *
 * <p>服务器无法直连 api.bgm.tv（GFW/IP 级拦截），磁盘缓存由本机脚本
 * scripts/fetch_and_upload_calendar.py 走代理拉取后 SFTP 上传（路径与
 * 服务运行目录下 data/ 相对路径一致，服务器为 /opt/chuxi/data/bangumi-calendar.json）。
 */
@Service
public class BangumiCalendarService {

    private static final Logger log = LoggerFactory.getLogger(BangumiCalendarService.class);

    private static final String API_BASE = "https://api.bgm.tv";
    private static final long CALENDAR_TTL = 30 * 60 * 1000L; // 30 分钟

    /** 磁盘缓存：相对服务运行目录（服务器 /opt/chuxi -> /opt/chuxi/data/bangumi-calendar.json） */
    private static final Path DISK_CACHE_PATH = Paths.get("data", "bangumi-calendar.json");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .proxy(ProxySelector.getDefault())
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    /** 可选 bgm.tv access token（提高限流配额），由 app.bangumi.token 注入 */
    private final String token;

    private volatile String calendarCache;
    private volatile long calendarCacheTime;

    public BangumiCalendarService(@Value("${app.bangumi.token:}") String token) {
        this.token = token == null ? "" : token.trim();
    }

    @PostConstruct
    public void initDiskCache() {
        try {
            Path parent = DISK_CACHE_PATH.getParent();
            if (parent != null) Files.createDirectories(parent);
        } catch (Exception e) {
            log.warn("Could not create disk cache dir: {}", e.getMessage());
        }
    }

    /** 获取放送日历 JSON 数组；任何情况下不抛异常（最终兜底空数组） */
    public JsonNode fetchCalendar() {
        long now = System.currentTimeMillis();
        // 1) 内存缓存
        if (calendarCache != null && (now - calendarCacheTime) < CALENDAR_TTL) {
            return parseSafe(calendarCache);
        }
        // 2) 磁盘缓存（api.bgm.tv 被墙时由本机脚本预置）
        String disk = readDiskCache();
        if (disk != null) {
            calendarCache = disk;
            calendarCacheTime = now;
            return parseSafe(disk);
        }
        // 3) 直连兜底（仅完全无缓存时）
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/calendar"))
                    .header("User-Agent", "chuxi-web/1.0 (https://www.chuxi.online)")
                    .timeout(Duration.ofSeconds(5))
                    .GET();
            if (!token.isEmpty()) {
                builder.header("Authorization", "Bearer " + token);
            }
            HttpResponse<String> resp = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                calendarCache = resp.body();
                calendarCacheTime = now;
                persistDiskCache(calendarCache);
                return parseSafe(calendarCache);
            }
            log.warn("Bangumi calendar API returned status {}", resp.statusCode());
        } catch (Exception e) {
            log.warn("Bangumi calendar live fetch failed: {} (will serve cached data)", e.getMessage());
        }
        // 4) 最后兜底：内存残值 / 空数组
        if (calendarCache != null) {
            JsonNode n = parseSafe(calendarCache);
            if (n != null) return n;
        }
        return mapper.createArrayNode();
    }

    private JsonNode parseSafe(String body) {
        try {
            return mapper.readTree(body);
        } catch (Exception e) {
            log.warn("Bangumi calendar cache parse failed: {}", e.getMessage());
            return null;
        }
    }

    private void persistDiskCache(String body) {
        try {
            Files.writeString(DISK_CACHE_PATH, body, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Failed to write Bangumi calendar disk cache: {}", e.getMessage());
        }
    }

    private String readDiskCache() {
        try {
            if (Files.exists(DISK_CACHE_PATH)) {
                return Files.readString(DISK_CACHE_PATH, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.warn("Failed to read Bangumi calendar disk cache: {}", e.getMessage());
        }
        return null;
    }
}
