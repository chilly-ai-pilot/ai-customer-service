package com.aicustomer.config;

import com.aicustomer.websocket.task.HeartbeatTask;
import com.aicustomer.websocket.task.TokenValidationTask;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * WebSocket 定时任务初始化配置。
 * 配合 @EnableScheduling 注解，让 HeartbeatTask 和 TokenValidationTask 的
 * @Scheduled 方法生效（每 30 秒自动执行）。
 */
@Slf4j
@Configuration
@EnableScheduling
public class WebSocketTaskInitializer {

    @PostConstruct
    public void init() {
        log.info("WebSocket 定时任务已启动：HeartbeatTask（心跳）& TokenValidationTask（token 校验）");
    }
}
