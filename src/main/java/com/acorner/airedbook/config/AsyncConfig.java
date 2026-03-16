package com.acorner.airedbook.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "videoTaskExecutor")
    public Executor videoTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 假设你的电脑/服务器是 4 核，我们限制最多跑 3 个视频任务
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(3);
        // 如果同时有超过 3 个任务，剩下的就排队，队列最多能排 50 个
        executor.setQueueCapacity(50);
        // 给这个线程池里的线程起个名字，以后万一报错了，看日志一眼就能认出来
        executor.setThreadNamePrefix("VideoWorker-");
        // 拒绝策略：如果连 50 个排队的位子都满了，就直接报错拒绝（保护服务器不崩溃）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}