package com.chuxi.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** PBKDF2 加盐哈希：存储格式 pbkdf2$迭代次数$盐$哈希（均为 Base64） */
public final class PasswordHasher {

    private static final String ALGO = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {}

    public static String hash(String raw) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] dk = derive(raw, salt, ITERATIONS);
        Base64.Encoder enc = Base64.getEncoder();
        return "pbkdf2$" + ITERATIONS + "$" + enc.encodeToString(salt) + "$" + enc.encodeToString(dk);
    }

    /** 校验；stored 若不是 pbkdf2 格式则按明文兼容比对（便于旧数据平滑迁移） */
    public static boolean matches(String raw, String stored) {
        if (stored == null || stored.isEmpty()) return false;
        if (!stored.startsWith("pbkdf2$")) {
            return MessageDigest.isEqual(raw.getBytes(), stored.getBytes());
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 4) return false;
        try {
            int iterations = Integer.parseInt(parts[1]);
            Base64.Decoder dec = Base64.getDecoder();
            byte[] salt = dec.decode(parts[2]);
            byte[] expected = dec.decode(parts[3]);
            return MessageDigest.isEqual(derive(raw, salt, iterations), expected);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isHashed(String stored) {
        return stored != null && stored.startsWith("pbkdf2$");
    }

    private static byte[] derive(String raw, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(raw.toCharArray(), salt, iterations, KEY_LENGTH);
            return SecretKeyFactory.getInstance(ALGO).generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("密码哈希计算失败", e);
        }
    }
}
