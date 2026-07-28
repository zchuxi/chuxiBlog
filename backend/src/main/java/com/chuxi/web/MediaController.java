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
