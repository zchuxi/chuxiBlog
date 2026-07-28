package com.lxblog.web;

import com.lxblog.common.R;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 图片库：上传 / 公开读取 / 列表 / 删除。
 * 文件存相对路径 uploads/ 目录；读取走 Controller 返回字节，
 * 不注册静态资源目录（中文路径 toUri() 百分号编码有坑）。
 */
@RestController
public class MediaController {

    private static final Path ROOT = Paths.get("uploads");

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

    /** 上传：文件名 = UUID 前 8 位 + "-" + 原名清洗（只留字母数字._-） */
    @PostMapping("/api/admin/upload")
    public R<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return R.fail("文件为空");
        Files.createDirectories(ROOT);
        String origin = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String cleaned = origin.replaceAll("[^A-Za-z0-9._-]", "_");
        if (cleaned.isBlank() || cleaned.chars().allMatch(c -> c == '.')) cleaned = "file";
        String name = UUID.randomUUID().toString().substring(0, 8) + "-" + cleaned;
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

    /** 列表：按 lastModified 倒序 */
    @GetMapping("/api/admin/media")
    public R<List<Map<String, Object>>> list() throws IOException {
        if (!Files.isDirectory(ROOT)) return R.ok(List.of());
        try (Stream<Path> stream = Files.list(ROOT)) {
            List<Map<String, Object>> items = stream
                    .filter(Files::isRegularFile)
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
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .sorted((a, b) -> Long.compare((Long) b.get("lastModified"), (Long) a.get("lastModified")))
                    .collect(Collectors.toList());
            return R.ok(items);
        }
    }

    /** 删除：同样防穿越 */
    @DeleteMapping("/api/admin/media/{name}")
    public R<Boolean> remove(@PathVariable String name) throws IOException {
        if (badName(name)) return R.fail("非法文件名");
        Path path = resolveSafe(name);
        if (path == null || !Files.isRegularFile(path)) return R.fail("文件不存在");
        Files.delete(path);
        return R.ok(true);
    }
}
