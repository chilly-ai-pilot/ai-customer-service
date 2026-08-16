package com.aicustomer.config;

import com.aicustomer.websocket.task.HeartbeatTask;
import com.aicustomer.websocket.task.TokenValidationTask;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@Configuration
@EnableScheduling
public class WebSocketTaskInitializer {

    @PostConstruct
    public void init() {
        log.info("WebSocket scheduled tasks initialized: HeartbeatTask and TokenValidationTask");
    }
}
