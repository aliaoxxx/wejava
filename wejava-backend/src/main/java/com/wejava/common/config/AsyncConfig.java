package com.wejava.common.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 异步线程池。目前只用于登录日志落库，避免写库拖慢登录响应。
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    /** AI 流式对话专用线程池：每个进行中的对话占一个线程 */
    public static final String AI_EXECUTOR = "aiExecutor";

    public static final String LOGIN_LOG_EXECUTOR = "loginLogExecutor";

    @Bean(LOGIN_LOG_EXECUTOR)
    public Executor loginLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("login-log-");
        // 队列满时丢弃最旧的任务：日志可以丢，登录不能阻塞
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        executor.initialize();
        return executor;
    }

    @Bean(AI_EXECUTOR)
    public ExecutorService aiExecutor() {
        return new ThreadPoolExecutor(
                4,
                32,
                60L,
                java.util.concurrent.TimeUnit.SECONDS,
                new java.util.concurrent.LinkedBlockingQueue<>(200),
                runnable -> {
                    Thread thread = new Thread(runnable, "ai-stream");
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.AbortPolicy());
    }
}
