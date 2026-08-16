package com.aicustomer.websocket.task;

import com.aicustomer.constant.SubjectType;
import com.aicustomer.websocket.ChatConnection;
import com.aicustomer.websocket.ConnectionPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.PingMessage;

import java.util.Map;

/**
 * 心跳任务：使用 WebSocket 协议原生的 ping/pong 帧检测连接是否存活。
 *
 * 机制说明：
 * - 服务端每 30 秒向所有活跃连接发送一个原生 Ping 帧
 * - 浏览器收到后自动回复 Pong 帧（对 JS 完全透明，JS 无法感知）
 * - 若连接实际已断开（网络中断等），sendMessage 会失败，此时关闭该连接
 *
 * 注意：ping/pong 不经过业务消息的编解码逻辑，不混进业务消息流。
 */
@Slf4j
@Component
public class HeartbeatTask {

    /** 心跳间隔：30 秒 */
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
                Long subjectId = entry.getKey();

                // 过滤已关闭的连接，避免 sendMessage 抛异常
                if (!conn.isOpen()) {
                    connectionPool.remove(type, subjectId);
                    continue;
                }

                try {
                    conn.getSession().sendMessage(ping);
                    log.debug("Sent ping to {} {}", type, subjectId);
                } catch (Exception e) {
                    log.warn("Ping failed for {} {}, closing connection: {}", type, subjectId, e.getMessage());
                    conn.close(4003, "ping timeout");
                    connectionPool.remove(type, subjectId);
                }
            }
        }
    }
}
