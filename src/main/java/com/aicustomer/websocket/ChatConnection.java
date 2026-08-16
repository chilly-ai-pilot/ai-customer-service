package com.aicustomer.websocket;

import com.aicustomer.constant.SubjectType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    public void close(int code, String reason) {
        if (session.isOpen()) {
            try {
                session.close(new CloseStatus(code, reason));
            } catch (IOException e) {
                log.error("Failed to close session", e);
            }
        }
    }

    public boolean isOpen() {
        return session.isOpen();
    }
}
