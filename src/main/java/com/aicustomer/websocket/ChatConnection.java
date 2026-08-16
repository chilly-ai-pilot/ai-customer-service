package com.aicustomer.websocket;

import com.aicustomer.constant.SubjectType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

/**
 * WebSocket 连接的业务封装，持有协议层 Session 并附加业务身份信息。
 * 不承担连接管理职责，只负责发送消息和关闭连接。
 */
@Slf4j
public class ChatConnection {

    private final WebSocketSession session;
    private final String token;
    private final Long subjectId;
    private final SubjectType subjectType;

    public ChatConnection(WebSocketSession session, String token, Long subjectId, SubjectType subjectType) {
        this.session = session;
        this.token = token;
        this.subjectId = subjectId;
        this.subjectType = subjectType;
    }

    public WebSocketSession getSession() {
        return session;
    }

    public String getToken() {
        return token;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public SubjectType getSubjectType() {
        return subjectType;
    }

    /**
     * 使用指定关闭码和原因关闭连接。
     *
     * @param code   关闭码（RFC6455 标准码或私有区间自定义码）
     * @param reason 关闭原因描述
     */
    public void close(int code, String reason) {
        if (session.isOpen()) {
            try {
                session.close(new CloseStatus(code, reason));
            } catch (IOException e) {
                log.error("关闭连接时发生异常", e);
            }
        }
    }

    /**
     * 判断连接是否仍处于打开状态。
     *
     * @return 连接是否打开
     */
    public boolean isOpen() {
        return session.isOpen();
    }
}
