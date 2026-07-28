package com.chuxi.web;

import com.chuxi.common.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * 媒体库（图片/音频）：上传 / 公开读取 / 列表 / 删除。
 * 配齐阿里云 OSS 后上传自动存 OSS（公共读 bucket，直返公网 URL）；
 * 未配置时回退本地 uploads/ 目录，存量本地文件始终可读、可删。
 */
@RestController
public class MediaController {

    private static final Logger log = LoggerFactory.getLogger(MediaController.class);

    private static final Path ROOT = Paths.get("uploads");

    private final OssStorageService oss;

    public MediaController(OssStorageService oss) {
        this.oss = oss;
    }

    /** 文件名穿越校验：拒绝含 / \ .. 的名字 */
    private static boolean badName(String name) {
        return name == null || name.isBlank()
                || name.contains("/") || name.contains("\\") || name.contains("..");
    }

    /** 解析并确保落在 uploads/ 内 */
    private static Path resolveSafe(String name) {
        Path p = ROOT.resolve(name).toAbsolutePath().normalize();
        if (!p.startsWith(ROOT.toAbsolutePath().normalize())) return null;
        return p;
    }

    /** 上传：文件名 = UUID 前 8 位 + "-" + 原名清洗（只留字母数字._-）；OSS 优先，未配置则落本地 */
    @PostMapping("/api/admin/upload")
    public R<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return R.fail("文件为空");
        String origin = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String cleaned = origin.replaceAll("[^A-Za-z0-9._-]", "_");
        if (cleaned.isBlank() || cleaned.chars().allMatch(c -> c == '.')) cleaned = "file";
        String name = UUID.randomUUID().toString().substring(0, 8) + "-" + cleaned;

        if (oss.available()) {
            try {
                return R.ok(oss.upload(name, file.getInputStream(), file.getSize(), file.getContentType()));
            } catch (Exception e) {
                log.error("OSS 上传失败：name={}, size={}, contentType={}", name, file.getSize(), file.getContentType(), e);
                return R.fail("OSS 上传失败：" + e.getMessage());
            }
        }

        Files.createDirectories(ROOT);
        Path target = resolveSafe(name);
        if (target == null) return R.fail("非法文件名");
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", name);
        data.put("url", "/api/uploads/" + name);
        data.put("size", Files.size(target));
        return R.ok(data);
    }

    /**
     * 取回外链图：仅接受本站 OSS 公网域（白名单防 SSRF），下载后重传到 OSS。
     * 用途：管理端「裁切」按钮对 OSS 图（canvas CORS 污染无法导出）时，先走这里转一份站内副本再裁。
     */
    @PostMapping("/api/admin/media/fetch")
    public R<Map<String, Object>> fetch(@RequestBody Map<String, String> body) throws IOException {
        if (!oss.available()) return R.fail("OSS 未配置，暂不支持取回外链");
        String url = body == null ? null : body.get("url");
        if (url == null || url.isBlank()) return R.fail("url 必填");

        String host = oss.publicHost();
        if (host == null || !url.startsWith(host + "/")) return R.fail("仅支持本站 OSS 公网地址");

        String originName;
        try {
            String path = java.net.URI.create(url).getPath();
            originName = path == null ? "" : path.substring(path.lastIndexOf('/') + 1);
        } catch (Exception e) {
            return R.fail("非法 url");
        }
        if (originName.isBlank()) return R.fail("无法从 url 推断文件名");
        // 清洗 + 加前缀，避免重名覆盖
        String cleaned = originName.replaceAll("[^A-Za-z0-9._-]", "_");
        if (cleaned.isBlank() || cleaned.chars().allMatch(c -> c == '.')) cleaned = "image";
        String name = UUID.randomUUID().toString().substring(0, 8) + "-fetch-" + cleaned;

        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) java.net.URI.create(url).toURL().openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(15000);
        conn.setRequestMethod("GET");
        conn.setInstanceFollowRedirects(false); // 重定向前一律拒绝，避免被引导到内网
        int code = conn.getResponseCode();
        if (code / 100 != 2) {
            conn.disconnect();
            return R.fail("下载失败，HTTP " + code);
        }
        long size = conn.getContentLengthLong();
        String contentType = conn.getContentType();
        try (java.io.InputStream in = conn.getInputStream()) {
            return R.ok(oss.upload(name, in, size, contentType));
        } finally {
            conn.disconnect();
        }
    }

    /** 公开读取（不在 /api/admin 下）：字节直出 + 7 天缓存 */
    @GetMapping("/api/uploads/{name}")
    public ResponseEntity<byte[]> serve(@PathVariable String name) throws IOException {
        if (badName(name)) return ResponseEntity.badRequest().build();
        Path path = resolveSafe(name);
        if (path == null || !Files.isRegularFile(path)) return ResponseEntity.notFound().build();
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            String probed = Files.probeContentType(path);
            if (probed != null) mediaType = MediaType.parseMediaType(probed);
        } catch (Exception ignored) {
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .body(Files.readAllBytes(path));
    }

    /** 列表：OSS 与本地存量合并，按 lastModified 倒序 */
    @GetMapping("/api/admin/media")
    public R<List<Map<String, Object>>> list() throws IOException {
        List<Map<String, Object>> items = new java.util.ArrayList<>();
        if (oss.available()) {
            try {
                items.addAll(oss.list());
            } catch (Exception e) {
                log.error("OSS 列表获取失败", e);
                return R.fail("OSS 列表获取失败：" + e.getMessage());
            }
        }
        if (Files.isDirectory(ROOT)) {
            try (Stream<Path> stream = Files.list(ROOT)) {
                stream.filter(Files::isRegularFile)
                        .map(p -> {
                            try {
                                Map<String, Object> m = new LinkedHashMap<>();
                                String fname = p.getFileName().toString();
                                m.put("name", fname);
                                m.put("url", "/api/uploads/" + fname);
                                m.put("size", Files.size(p));
                                m.put("lastModified", Files.getLastModifiedTime(p).toMillis());
                                return m;
                            } catch (IOException e) {
                                log.warn("读取本地媒体文件信息失败，已跳过：path={}, err={}", p, e.getMessage());
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .forEach(items::add);
            }
        }
        items.sort((a, b) -> Long.compare((Long) b.get("lastModified"), (Long) a.get("lastModified")));
        return R.ok(items);
    }

    /** 删除：先试 OSS，再试本地；同样防穿越 */
    @DeleteMapping("/api/admin/media/{name}")
    public R<Boolean> remove(@PathVariable String name) throws IOException {
        if (badName(name)) return R.fail("非法文件名");
        if (oss.available()) {
            try {
                if (oss.exists(name)) {
                    oss.remove(name);
                    return R.ok(true);
                }
            } catch (Exception e) {
                log.error("OSS 删除失败：name={}", name, e);
                return R.fail("OSS 删除失败：" + e.getMessage());
            }
        }
        Path path = resolveSafe(name);
        if (path == null || !Files.isRegularFile(path)) return R.fail("文件不存在");
        Files.delete(path);
        return R.ok(true);
    }
}
