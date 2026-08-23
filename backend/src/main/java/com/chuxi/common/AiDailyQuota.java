package com.chuxi.common;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * AI 上游调用的全站每日配额。
 * <p>
 * 存在理由：/api/front/ai/chat 是公开接口，消耗的是站方付费 key。原先只有
 * 「每 IP 每分钟 10 次」的限流，换 IP 即可绕过，成本没有上限。此处再加一道
 * 与来源无关的全站日闸，作为费用兜底。
 * <p>
 * 不复用 {@link RateLimiter}：它的惰性清扫以 1 小时（{@code MAX_WINDOW_MS}）
 * 判定桶是否可安全回收，而日配额窗口是 24 小时。伪造 IP 洪水会把 map 顶到
 * 清扫阈值，此时全局配额桶可能因「最后一次记录早于 1 小时」被误删、配额被
 * 重置——恰好在最需要它的时候失效。这里改用「计数 + 日期」，O(1) 内存，
 * 与清扫逻辑无关。
 * <p>
 * 单机内存态：重启清零、多实例不共享，与站内既有限流器的口径一致（单实例部署）。
 */
@Component
public class AiDailyQuota {

    /** 与站内其他日期处理保持一致的时区，避免 UTC 跨日导致配额在当地下午重置 */
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final Object lock = new Object();
    private LocalDate day = LocalDate.now(ZONE);
    private int used = 0;

    /**
     * 尝试占用一次上游调用额度。
     *
     * @param maxPerDay 每日上限；小于等于 0 表示不限制
     * @return true 表示允许调用上游，false 表示当日额度已用尽
     */
    public boolean tryAcquire(int maxPerDay) {
        if (maxPerDay <= 0) return true;
        LocalDate today = LocalDate.now(ZONE);
        synchronized (lock) {
            if (!today.equals(day)) {
                day = today;
                used = 0;
            }
            if (used >= maxPerDay) return false;
            used++;
            return true;
        }
    }

    /** 当日已使用次数，供后台观测 */
    public int used() {
        LocalDate today = LocalDate.now(ZONE);
        synchronized (lock) {
            return today.equals(day) ? used : 0;
        }
    }

    /** 仅用于测试，清空当日计数 */
    public void reset() {
        synchronized (lock) {
            day = LocalDate.now(ZONE);
            used = 0;
        }
    }
}
