package com.aicustomer.websocket.task;

import com.aicustomer.constant.SubjectType;
import com.aicustomer.service.TokenService;
import com.aicustomer.websocket.ChatConnection;
import com.aicustomer.websocket.ConnectionPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Token 校验任务：定期检查所有 WS 连接绑定的 token 是否仍然有效。
 *
 * 机制说明：
 * - 与心跳判活完全独立：token 失效（如被重复登录顶替）不需要等下一次 ping/pong 才能判断
 * - 每 30 秒遍历一次连接池，用 TokenService 校验 token
 * - token 失效或与连接池身份不匹配时主动关闭连接（关闭码 4004）
 *
 * 与心跳任务的区别：
 * - 心跳任务：检测连接"物理层"是否存活（对方网络是否通）
 * - 本任务：检测连接"业务层"身份是否还有效（token 是否被作废）
 */
@Slf4j
@Component
public class TokenValidationTask {

    /** 校验间隔：30 秒 */
    private static final int CHECK_INTERVAL_MS = 30_000;

    @Autowired
    private ConnectionPool connectionPool;

    @Autowired
    private TokenService tokenService;

    @Scheduled(fixedRate = CHECK_INTERVAL_MS)
    public void validateTokens() {
        for (SubjectType type : SubjectType.values()) {
            Map<Long, ChatConnection> pool = connectionPool.allConnectionsOf(type);
            for (Map.Entry<Long, ChatConnection> entry : pool.entrySet()) {
                Long subjectId = entry.getKey();
                ChatConnection conn = entry.getValue();

                // 过滤已关闭的连接
                if (!conn.isOpen()) {
                    connectionPool.remove(type, subjectId);
                    continue;
                }

                // 用 tokenService 解析 token，验证身份有效性
                String token = conn.getToken();
                Long resolved = tokenService.resolve(type, token);
                if (resolved == null || !resolved.equals(subjectId)) {
                    log.info("Token expired/invalid for {} {}, closing connection", type, subjectId);
                    conn.close(4004, "token expired");
                    connectionPool.remove(type, subjectId);
                }
            }
        }
    }
}
