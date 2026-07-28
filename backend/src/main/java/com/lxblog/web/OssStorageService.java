package com.lxblog.web;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ListObjectsV2Request;
import com.aliyun.oss.model.ListObjectsV2Result;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectMetadata;
import com.lxblog.config.OssProperties;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 阿里云 OSS 存储服务：上传 / 列表 / 删除。
 * bucket 为公共读，返回公网 URL 直接给前端 <img>/<audio> 使用。
 */
@Service
public class OssStorageService {

    private final OssProperties props;
    private volatile OSS client;

    public OssStorageService(OssProperties props) {
        this.props = props;
    }

    public boolean available() {
        return props.ready();
    }

    private OSS client() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = new OSSClientBuilder()
                            .build(props.getEndpoint(), props.getAccessKeyId(), props.getAccessKeySecret());
                }
            }
        }
        return client;
    }

    /** 归一化前缀：非空时保证以 / 结尾、不以 / 开头 */
    private String dir() {
        String d = props.getDir() == null ? "" : props.getDir().trim();
        if (d.startsWith("/")) d = d.substring(1);
        if (!d.isEmpty() && !d.endsWith("/")) d += "/";
        return d;
    }

    /** 对象名 -> 公网访问 URL（优先自定义域名，否则 bucket.endpoint 三级域名） */
    public String publicUrl(String objectKey) {
        String host = props.getPublicHost() == null ? "" : props.getPublicHost().trim();
        if (!host.isEmpty()) {
            if (host.endsWith("/")) host = host.substring(0, host.length() - 1);
            if (!host.startsWith("http")) host = "https://" + host;
            return host + "/" + objectKey;
        }
        String endpointHost = URI.create(props.getEndpoint()).getHost();
        return "https://" + props.getBucket() + "." + endpointHost + "/" + objectKey;
    }

    /** 上传：返回 { name(对象名去前缀), url, size } */
    public Map<String, Object> upload(String fileName, InputStream in, long size, String contentType) {
        String key = dir() + fileName;
        ObjectMetadata meta = new ObjectMetadata();
        if (size > 0) meta.setContentLength(size);
        if (contentType != null && !contentType.isBlank()) meta.setContentType(contentType);
        // 公共读 bucket 默认 7 天浏览器缓存，与本地实现口径一致
        meta.setCacheControl("public, max-age=604800");
        client().putObject(props.getBucket(), key, in, meta);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", fileName);
        data.put("url", publicUrl(key));
        data.put("size", size);
        return data;
    }

    /** 列表：分页拉全 dir 前缀下对象，按修改时间倒序 */
    public List<Map<String, Object>> list() {
        List<Map<String, Object>> items = new ArrayList<>();
        String prefix = dir();
        String token = null;
        do {
            ListObjectsV2Request req = new ListObjectsV2Request(props.getBucket())
                    .withPrefix(prefix)
                    .withMaxKeys(1000)
                    .withContinuationToken(token);
            ListObjectsV2Result result = client().listObjectsV2(req);
            for (OSSObjectSummary s : result.getObjectSummaries()) {
                if (s.getKey().endsWith("/")) continue; // 跳过目录占位对象
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", s.getKey().substring(prefix.length()));
                m.put("url", publicUrl(s.getKey()));
                m.put("size", s.getSize());
                m.put("lastModified", s.getLastModified() == null ? 0L : s.getLastModified().getTime());
                items.add(m);
            }
            token = result.isTruncated() ? result.getNextContinuationToken() : null;
        } while (token != null);
        items.sort((a, b) -> Long.compare((Long) b.get("lastModified"), (Long) a.get("lastModified")));
        return items;
    }

    public boolean exists(String fileName) {
        return client().doesObjectExist(props.getBucket(), dir() + fileName);
    }

    public void remove(String fileName) {
        client().deleteObject(props.getBucket(), dir() + fileName);
    }

    @PreDestroy
    public void shutdown() {
        if (client != null) client.shutdown();
    }
}
