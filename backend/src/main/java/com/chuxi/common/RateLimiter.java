package com.chuxi.common;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 轻量级 IP 级滑动窗口频率限制器。
 * <p>
 * 在 {@code windowSeconds} 内最多允许 {@code maxRequests} 次请求。
 * 线程安全，适用于低并发场景。
 * <p>
 * 内存治理：静态 map 只保留「窗口内活跃」的 key；当 map 规模超过阈值时，
 * 惰性清扫长时间无请求的闲置桶，防止伪造 IP 洪水导致 map 无限增长。
 */
public final class RateLimiter {

    private static final int DEFAULT_WINDOW_SECONDS = 60;
    private static final int DEFAULT_MAX_REQUESTS = 3;

    /** 最大窗口（bump 用 3600s）；闲置超过该时长的桶可安全清理（其记录必然全部过期） */
    private static final long MAX_WINDOW_MS = 3600 * 1000L;
    /** 触发清扫的 map 规模阈值 */
    private static final int SWEEP_THRESHOLD = 4096;
    /** 清扫最小间隔，避免高频调用时反复全量遍历 */
    private static final long SWEEP_MIN_INTERVAL_MS = 60 * 1000L;

    private static final ConcurrentHashMap<String, Deque<Long>> RECORDS = new ConcurrentHashMap<>();
    private static volatile long lastSweepAt = 0;

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
        sweepIfNeeded();
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

    /**
     * map 规模超阈值时惰性清扫：移除「最新记录也早于最大窗口」的闲置桶。
     * 被清理的 key 其记录必然全部过期，删除不影响任何后续限流判断；
     * 每次调用只做一次 size 检查与 volatile 读，开销可忽略。
     */
    private static void sweepIfNeeded() {
        if (RECORDS.size() < SWEEP_THRESHOLD) return;
        long now = System.currentTimeMillis();
        if (now - lastSweepAt < SWEEP_MIN_INTERVAL_MS) return;
        lastSweepAt = now;
        RECORDS.forEach((key, dq) -> {
            synchronized (dq) {
                if (dq.isEmpty() || now - dq.peekLast() > MAX_WINDOW_MS) {
                    // 条件移除：仅当该 key 仍映射到当前桶时才删除，避免误删并发新建的桶
                    RECORDS.remove(key, dq);
                }
            }
        });
    }

    /** 仅用于测试，清空所有记录 */
    public static void reset() {
        RECORDS.clear();
        lastSweepAt = 0;
    }
}
