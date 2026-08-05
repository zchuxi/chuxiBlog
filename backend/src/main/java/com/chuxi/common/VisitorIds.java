package com.chuxi.common;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * 匿名访客标识：服务端 HMAC 签名签发/验签。
 * <p>
 * 请求头格式 {@code <rawId>.<sig>}，sig = HMAC-SHA256(rawId, secret)。
 * 客户端无法自行构造合法标识（没有服务端密钥），只能通过签发接口获取；
 * 签名部分仅用于请求头传输，入库时剥离（数据库列长度不变）。
 * 密钥由启动流程初始化并持久化到 site_content，重启不失效。
 */
public final class VisitorIds {

    /** rawId 部分：与历史格式一致，16-64 位安全字符 */
    public static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9_-]{16,64}");
    /** 完整签名 token：rawId 16-64 + 点 + 64 位 hex 签名 */
    private static final Pattern SIGNED_TOKEN = Pattern.compile("^([A-Za-z0-9_-]{16,64})\\.([0-9a-f]{64})$");

    private static volatile byte[] secret;

    private VisitorIds() {}

    /** 由启动流程注入服务端密钥（持久化于 site_content，重启不失效） */
    public static void init(byte[] key) {
        if (key == null || key.length < 16) {
            throw new IllegalArgumentException("visitor secret 至少 16 字节");
        }
        secret = key.clone();
    }

    public static boolean isInitialized() {
        return secret != null;
    }

    /** 生成新的 rawId（32 位十六进制，符合 16-64 安全字符规则） */
    public static String newRawId() {
        byte[] buf = new byte[16];
        new SecureRandom().nextBytes(buf);
        return HexFormat.of().formatHex(buf);
    }

    /** 为 rawId 签发签名 token：{rawId}.{hexSig} */
    public static String issue(String rawId) {
        byte[] key = requireSecret();
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            String sig = HexFormat.of().formatHex(mac.doFinal(rawId.getBytes(StandardCharsets.UTF_8)));
            return rawId + "." + sig;
        } catch (Exception e) {
            throw new IllegalStateException("visitor token 签发失败", e);
        }
    }

    /** 验签：token 合法返回 rawId，否则返回 null（fail-closed） */
    public static String resolve(String token) {
        if (token == null) return null;
        var m = SIGNED_TOKEN.matcher(token);
        if (!m.matches()) return null;
        String rawId = m.group(1);
        String sig = m.group(2);
        byte[] key = requireSecret();
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal(rawId.getBytes(StandardCharsets.UTF_8)));
            // 常量时间比较，避免时序侧信道
            if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), sig.getBytes(StandardCharsets.UTF_8))) {
                return null;
            }
            return rawId;
        } catch (Exception e) {
            return null;
        }
    }

    /** 兼容旧调用语义：token 有效即 true */
    public static boolean isValid(String token) {
        return resolve(token) != null;
    }

    private static byte[] requireSecret() {
        byte[] key = secret;
        if (key == null) {
            throw new IllegalStateException("visitor secret 未初始化");
        }
        return key;
    }
}
