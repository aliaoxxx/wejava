package com.wejava.common.interceptor;

import com.wejava.system.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 并发上限解析器。
 * <p>
 * 参数存在 sys_config（sys.request.maxConcurrency），但每个请求都查库代价太高，
 * 这里做 30 秒内存缓存；查不到或查询失败时回退到配置文件的默认值。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConcurrencyLimitResolver {

    public static final String CONFIG_KEY = "sys.request.maxConcurrency";

    private static final long CACHE_TTL_MS = 30_000L;

    private final SysConfigService configService;

    @Value("${wejava.concurrency.default-limit:10}")
    private int defaultLimit;

    private volatile int cachedLimit = -1;
    private volatile long loadedAt = 0L;

    public int getLimit() {
        long now = System.currentTimeMillis();
        if (cachedLimit >= 0 && now - loadedAt < CACHE_TTL_MS) {
            return cachedLimit;
        }
        synchronized (this) {
            if (cachedLimit >= 0 && now - loadedAt < CACHE_TTL_MS) {
                return cachedLimit;
            }
            cachedLimit = load();
            loadedAt = now;
            return cachedLimit;
        }
    }

    /** 参数变更后可主动调用，立即生效 */
    public void evict() {
        loadedAt = 0L;
    }

    private int load() {
        try {
            String value = configService.getValueByKey(CONFIG_KEY);
            int parsed = Integer.parseInt(value.trim());
            return Math.max(parsed, 0);
        } catch (Exception e) {
            log.debug("读取并发上限参数失败，使用默认值 {}: {}", defaultLimit, e.getMessage());
            return defaultLimit;
        }
    }
}
