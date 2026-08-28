package com.chuxi.web;

import com.chuxi.common.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

    /** 允许上传的文件扩展名白名单 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp",
            ".mp3", ".ogg", ".wav", ".flac", ".aac"
    );

    /** 允许上传的 MIME 类型白名单 */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp",
            "audio/mpeg", "audio/ogg", "audio/wav", "audio/flac", "audio/aac",
            "audio/x-wav"
    );

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
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) return R.fail("文件名无效");
        String reject = typeRejectReason(file, originalFilename);
        if (reject != null) return R.fail(reject);

        String origin = originalFilename;
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
        try (InputStream is = file.getInputStream()) {
            Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", name);
        data.put("url", "/api/uploads/" + name);
        data.put("size", Files.size(target));
        return R.ok(data);
    }

    /**
     * 上传/覆盖共用的类型校验：扩展名白名单 + MIME 白名单 + 文件头 Magic Number。
     * 通过返回 null，否则返回中文失败原因。
     */
    private String typeRejectReason(MultipartFile file, String nameForExt) throws IOException {
        String lowerName = nameForExt.toLowerCase();
        boolean extOk = ALLOWED_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
        String contentType = file.getContentType();
        boolean mimeOk = contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase());
        if (!extOk || !mimeOk) return "不支持的文件类型，仅允许图片和音频文件";

        // 文件头 Magic Number 校验：只读头部 16 字节，验证文件真实内容与声明类型一致
        byte[] header = new byte[16];
        try (InputStream is = file.getInputStream()) {
            int read = is.read(header);
            if (read < 4) return "文件内容过短";
            header = Arrays.copyOf(header, read);
        }
        if (!validateMagicNumber(header, contentType)) return "文件内容与声明类型不符";
        return null;
    }

    /**
     * 覆盖场景专用：扩展名必须和新内容的 MIME 对得上。
     * 覆盖不改文件名，URL 里的扩展名一旦和内容不符，本地 serve 按扩展名探测出的
     * Content-Type 就是错的（OSS 同理），浏览器会拿到一张打不开的图。
     * 只放行图片——这个接口是给裁切用的，音频没有原地替换的场景。
     */
    private static boolean extMatchesType(String name, String contentType) {
        String lower = name.toLowerCase();
        String ct = contentType == null ? "" : contentType.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return ct.contains("jpeg");
        if (lower.endsWith(".png")) return ct.contains("png");
        if (lower.endsWith(".webp")) return ct.contains("webp");
        if (lower.endsWith(".gif")) return ct.contains("gif");
        if (lower.endsWith(".bmp")) return ct.contains("bmp");
        return false;
    }

    /**
     * 覆盖后 URL 不变但内容变了，而本地与 OSS 都挂了 7 天强缓存，浏览器不会回源。
     * 附一个版本参数让调用方拿到的地址能立刻看到新图。
     */
    private static String withVersion(String url) {
        if (url == null) return null;
        return url + (url.contains("?") ? "&" : "?") + "v=" + System.currentTimeMillis();
    }

    /**
     * 覆盖已有媒体文件：同名、同格式原地替换，用于裁切后直接覆盖原图。
     * 目标必须已存在——这个接口只做替换，不承担「顺手创建」的职责。
     */
    @PostMapping("/api/admin/media/{name}/replace")
    public R<Map<String, Object>> replace(@PathVariable String name,
                                          @RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return R.fail("文件为空");
        if (badName(name)) return R.fail("非法文件名");
        String reject = typeRejectReason(file, name);
        if (reject != null) return R.fail(reject);
        if (!extMatchesType(name, file.getContentType())) {
            return R.fail("覆盖要求格式与原文件一致，请改用「保存为新图」");
        }

        if (oss.available()) {
            try {
                // 对象不存在时不在 OSS 里造一个，落到下面的本地分支去找存量文件
                if (oss.exists(name)) {
                    Map<String, Object> data = new LinkedHashMap<>(
                            oss.upload(name, file.getInputStream(), file.getSize(), file.getContentType()));
                    data.put("url", withVersion((String) data.get("url")));
                    return R.ok(data);
                }
            } catch (Exception e) {
                log.error("OSS 覆盖失败：name={}, size={}", name, file.getSize(), e);
                return R.fail("OSS 覆盖失败：" + e.getMessage());
            }
        }

        Path target = resolveSafe(name);
        if (target == null) return R.fail("非法文件名");
        if (!Files.isRegularFile(target)) return R.fail("原文件不存在，无法覆盖");
        try (InputStream is = file.getInputStream()) {
            Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", name);
        data.put("url", withVersion("/api/uploads/" + name));
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
        // 远程 Content-Type 白名单：仅允许图片 MIME，杜绝非图片对象（如 text/html）被透传到 OSS 后以原类型直返浏览器
        String mediaType = conn.getContentType();
        if (mediaType != null) {
            int semi = mediaType.indexOf(';');
            mediaType = (semi >= 0 ? mediaType.substring(0, semi) : mediaType).trim().toLowerCase();
        }
        if (mediaType == null || !ALLOWED_FETCH_TYPES.contains(mediaType)) {
            conn.disconnect();
            return R.fail("仅支持图片类型（jpg/png/gif/webp/avif）");
        }
        if (size > MAX_FETCH_BYTES) {
            conn.disconnect();
            return R.fail("文件超过 25MB 限制");
        }
        try (java.io.InputStream in = boundedInput(conn.getInputStream())) {
            return R.ok(oss.upload(name, in, size, mediaType));
        } finally {
            conn.disconnect();
        }
    }

    /** fetch 外链取回：仅接受这几种图片 MIME，且单文件 ≤ 25MB */
    private static final java.util.Set<String> ALLOWED_FETCH_TYPES =
            java.util.Set.of("image/jpeg", "image/png", "image/gif", "image/webp", "image/avif");
    private static final long MAX_FETCH_BYTES = 25L * 1024 * 1024;

    /** 限制读取包装：防 chunked / 无 Content-Length 的响应被无限放大下载 */
    private static java.io.InputStream boundedInput(java.io.InputStream in) {
        final long[] read = {0};
        return new java.io.FilterInputStream(in) {
            @Override
            public int read() throws java.io.IOException {
                if (read[0] > MAX_FETCH_BYTES) throw new java.io.IOException("文件超过 25MB 限制");
                int b = super.read();
                if (b != -1) read[0]++;
                return b;
            }

            @Override
            public int read(byte[] b, int off, int len) throws java.io.IOException {
                if (read[0] > MAX_FETCH_BYTES) throw new java.io.IOException("文件超过 25MB 限制");
                len = (int) Math.min(len, MAX_FETCH_BYTES - read[0]);
                if (len <= 0) return -1;
                int n = super.read(b, off, len);
                if (n > 0) read[0] += n;
                return n;
            }
        };
    }

    /** 公开读取（不在 /api/admin 下）：流式响应 + 7 天缓存 */
    @GetMapping("/api/uploads/{name}")
    public ResponseEntity<Resource> serve(@PathVariable String name) throws IOException {
        if (badName(name)) return ResponseEntity.badRequest().build();
        Path path = resolveSafe(name);
        if (path == null || !Files.isRegularFile(path)) return ResponseEntity.notFound().build();
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            String probed = Files.probeContentType(path);
            if (probed != null) mediaType = MediaType.parseMediaType(probed);
        } catch (Exception ignored) {
        }
        Resource resource = new InputStreamResource(Files.newInputStream(path));
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(Files.size(path))
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=604800")
                .body(resource);
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

    /**
     * 文件头 Magic Number 校验：根据声明的 MIME 类型检查文件头字节是否匹配。
     * 未知图片/音频类型保守放行，已知类型必须匹配。
     */
    private boolean validateMagicNumber(byte[] header, String contentType) {
        if (header == null || header.length < 4) return false;
        if (contentType == null) return true;
        String ct = contentType.toLowerCase();

        // JPEG: FF D8 FF
        if (ct.contains("jpeg")) {
            return (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF;
        }
        // PNG: 89 50 4E 47
        if (ct.contains("png")) {
            return (header[0] & 0xFF) == 0x89 && (header[1] & 0xFF) == 0x50
                    && (header[2] & 0xFF) == 0x4E && (header[3] & 0xFF) == 0x47;
        }
        // GIF: 47 49 46 38
        if (ct.contains("gif")) {
            return header[0] == 'G' && header[1] == 'I' && header[2] == 'F' && header[3] == '8';
        }
        // WebP: RIFF....WEBP
        if (ct.contains("webp")) {
            return header.length >= 12
                    && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
        }
        // BMP: 42 4D
        if (ct.contains("bmp")) {
            return header[0] == 'B' && header[1] == 'M';
        }
        // WAV: RIFF....WAVE
        if (ct.contains("wav")) {
            return header.length >= 12
                    && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'A' && header[10] == 'V' && header[11] == 'E';
        }
        // FLAC: 66 4C 61 43
        if (ct.contains("flac")) {
            return header[0] == 'f' && header[1] == 'L' && header[2] == 'a' && header[3] == 'C';
        }
        // OGG: 4F 67 67 53
        if (ct.contains("ogg")) {
            return header[0] == 'O' && header[1] == 'g' && header[2] == 'g' && header[3] == 'S';
        }
        // MP3: ID3 标签（49 44 33）或 MPEG 帧同步（FF Ex）
        if (ct.contains("mpeg")) {
            return (header[0] == 'I' && header[1] == 'D' && header[2] == '3')
                    || ((header[0] & 0xFF) == 0xFF && (header[1] & 0xE0) == 0xE0);
        }
        // AAC: ADTS 同步字（FF Fx）
        if (ct.contains("aac")) {
            return (header[0] & 0xFF) == 0xFF && (header[1] & 0xF0) == 0xF0;
        }
        // 未知类型，放行（保守策略：白名单外的 MIME 已在 upload() 上层拦截，此处仅兜底）
        return true;
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
