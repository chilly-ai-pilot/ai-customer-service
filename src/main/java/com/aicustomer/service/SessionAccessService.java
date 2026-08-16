package com.aicustomer.service;

import com.aicustomer.context.CurrentUser;
import com.aicustomer.entity.Session;
import com.aicustomer.exception.ForbiddenException;
import com.aicustomer.exception.ResourceNotFoundException;
import com.aicustomer.repository.SessionRepository;
import org.springframework.stereotype.Service;

@Service
public class SessionAccessService {

    private final SessionRepository sessionRepository;

    public SessionAccessService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    /**
     * 校验当前用户是否有权访问该会话，返回已校验的 Session。
     * 逻辑：session 不存在 → 404；存在但非参与方 → 403。
     */
    public Session validateAndGetSession(Long sessionId, CurrentUser currentUser) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        boolean authorized = switch (currentUser.getType()) {
            case USER -> session.getUserId().equals(currentUser.getId());
            case TENANT -> session.getCtId().equals(currentUser.getId());
        };

        if (!authorized) {
            throw new ForbiddenException();
        }
        return session;
    }
}
