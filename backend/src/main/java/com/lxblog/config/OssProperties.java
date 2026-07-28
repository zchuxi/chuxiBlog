package com.lxblog.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OSS 配置项；AK/SK 通过环境变量注入。
 * enabled=true 且 AK/SK 齐全时视为可用，否则媒体上传回退本地磁盘。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssProperties {

    private boolean enabled = true;
    private String endpoint = "";
    private String bucket = "";
    private String accessKeyId = "";
    private String accessKeySecret = "";
    /** 对象名前缀（目录），如 media/ */
    private String dir = "media/";
    /** 自定义访问域名（CDN），留空用 bucket.endpoint 公网地址 */
    private String publicHost = "";

    public boolean ready() {
        return enabled
                && notBlank(endpoint) && notBlank(bucket)
                && notBlank(accessKeyId) && notBlank(accessKeySecret);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
