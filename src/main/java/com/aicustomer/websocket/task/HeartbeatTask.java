package com.aicustomer.websocket.task;

import com.aicustomer.constant.SubjectType;
import com.aicustomer.websocket.ChatConnection;
import com.aicustomer.websocket.ConnectionPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;

import java.util.Map;

@Slf4j
@Component
public class HeartbeatTask {

    private static final int PING_INTERVAL_MS = 30_000;

    @Autowired
    private ConnectionPool connectionPool;

    @Scheduled(fixedRate = PING_INTERVAL_MS)
    public void pingAll() {
        PingMessage ping = new PingMessage();
        for (SubjectType type : SubjectType.values()) {
            Map<Long, ChatConnection> pool = connectionPool.allConnectionsOf(type);
            for (Map.Entry<Long, ChatConnection> entry : pool.entrySet()) {
                ChatConnection conn = entry.getValue();
                if (!conn.isOpen()) {
                    connectionPool.remove(type, entry.getKey());
                    continue;
                }
                try {
                    conn.getSession().sendMessage(ping);
                    log.debug("Sent ping to {} {}", type, entry.getKey());
                } catch (Exception e) {
                    log.warn("Ping failed for {} {}, closing connection: {}", type, entry.getKey(), e.getMessage());
                    conn.close(4003, "ping timeout");
                    connectionPool.remove(type, entry.getKey());
                }
            }
        }
    }
}
