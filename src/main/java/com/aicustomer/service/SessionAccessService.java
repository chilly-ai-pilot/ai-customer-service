package com.aicustomer.service;

import com.aicustomer.context.CurrentUser;
import com.aicustomer.entity.Session;
import com.aicustomer.exception.ForbiddenException;
import com.aicustomer.exception.ResourceNotFoundException;
import com.aicustomer.repository.SessionRepository;
import org.springframework.stereotype.Service;

/**
 * 会话访问校验服务，提供会话归属校验逻辑。
 */
@Service
public class SessionAccessService {

    private final SessionRepository sessionRepository;

    public SessionAccessService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    /**
     * 校验当前用户是否有权访问该会话，返回已校验的 Session。
     *
     * @param sessionId    会话 ID
     * @param currentUser 当前登录用户
     * @return 会话实体
     * @throws ResourceNotFoundException 会话不存在
     * @throws ForbiddenException        当前用户不是该会话的参与方
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
