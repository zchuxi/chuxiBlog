package com.chuxi.common;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 轻量级 IP 级滑动窗口频率限制器。
 * <p>
 * 在 {@code windowSeconds} 内最多允许 {@code maxRequests} 次请求。
 * 线程安全，适用于低并发场景。
 */
public final class RateLimiter {

    private static final int DEFAULT_WINDOW_SECONDS = 60;
    private static final int DEFAULT_MAX_REQUESTS = 3;

    private static final ConcurrentHashMap<String, Deque<Long>> RECORDS = new ConcurrentHashMap<>();

    private RateLimiter() {}

    /**
     * 尝试获取一次许可。
     *
     * @param key            限流键（通常为 IP）
     * @param windowSeconds  窗口时长（秒）
     * @param maxRequests    窗口内最大请求数
     * @return true 表示允许，false 表示已超限
     */
    public static boolean tryAcquire(String key, int windowSeconds, int maxRequests) {
        long now = System.currentTimeMillis();
        long cutoff = now - windowSeconds * 1000L;
        Deque<Long> dq = RECORDS.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (dq) {
            // 移除窗口外的记录
            while (!dq.isEmpty() && dq.peekFirst() < cutoff) {
                dq.pollFirst();
            }
            if (dq.size() >= maxRequests) {
                return false;
            }
            dq.addLast(now);
            return true;
        }
    }

    /** 使用默认配置（60 秒 / 3 次）尝试获取许可 */
    public static boolean tryAcquire(String key) {
        return tryAcquire(key, DEFAULT_WINDOW_SECONDS, DEFAULT_MAX_REQUESTS);
    }

    /** 仅用于测试，清空所有记录 */
    public static void reset() {
        RECORDS.clear();
    }
}
