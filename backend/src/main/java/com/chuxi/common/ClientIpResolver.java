package com.chuxi.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 客户端真实 IP 解析。
 * <p>
 * 默认（app.trust-proxy=false，fail-closed）不信任任何客户端可控的转发头，
 * 直接返回 {@code getRemoteAddr()}。生产环境由受控反向代理（nginx）接入时，
 * 必须同时满足：① 设置 {@code APP_TRUST_PROXY=true}；② nginx 以
 * {@code proxy_set_header} 覆盖（而非追加）X-Forwarded-For / X-Real-IP；
 * ③ 应用端口不对公网开放。否则转发头可被客户端伪造，绕过基于 IP 的限流与审计。
 */
@Component
public class ClientIpResolver {

    private final boolean trustProxy;

    public ClientIpResolver(@Value("${app.trust-proxy:false}") boolean trustProxy) {
        this.trustProxy = trustProxy;
    }

    public String resolve(HttpServletRequest request) {
        if (trustProxy) {
            // 优先 X-Forwarded-For（取第一个，去除代理链）
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            // 其次 X-Real-IP
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }
        }
        // 默认（不信任代理）或无可信头时，回退到 TCP 对端地址
        return request.getRemoteAddr();
    }
}
