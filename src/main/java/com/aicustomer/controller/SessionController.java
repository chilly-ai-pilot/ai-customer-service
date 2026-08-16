package com.aicustomer.controller;

import com.aicustomer.context.CurrentUser;
import com.aicustomer.dto.response.ApiResponse;
import com.aicustomer.dto.response.SessionResponse;
import com.aicustomer.entity.Session;
import com.aicustomer.service.SessionAccessService;
import com.aicustomer.service.SessionService;
import com.aicustomer.service.TokenService;
import com.aicustomer.constant.SubjectType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * 会话管理 Controller，提供会话列表查询和会话详情接口。
 */
@RestController
@RequestMapping("/session")
@Tag(name = "会话管理", description = "用户与商户的聊天会话管理")
public class SessionController {

    private final TokenService tokenService;
    private final SessionService sessionService;
    private final SessionAccessService sessionAccess;

    public SessionController(TokenService tokenService,
                          SessionService sessionService,
                          SessionAccessService sessionAccess) {
        this.tokenService = tokenService;
        this.sessionService = sessionService;
        this.sessionAccess = sessionAccess;
    }

    /**
     * 商户查询自己的会话列表。
     * ctId 从 token 解析，不从参数传入。
     */
    @Operation(summary = "商户查询会话列表")
    @GetMapping("/ct/list")
    public ApiResponse<Page<SessionResponse>> listByTenant(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long ctId = tokenService.requireToken(SubjectType.TENANT, token);
        Page<SessionResponse> page = sessionService.listByTenant(ctId, pageNum, pageSize);
        return ApiResponse.success(page);
    }

    /**
     * 用户查询自己的会话列表。
     * userId 从 token 解析，不从参数传入。
     */
    @Operation(summary = "用户查询会话列表")
    @GetMapping("/user/list")
    public ApiResponse<Page<SessionResponse>> listByUser(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = tokenService.requireToken(SubjectType.USER, token);
        Page<SessionResponse> page = sessionService.listByUser(userId, pageNum, pageSize);
        return ApiResponse.success(page);
    }

    /**
     * 查询会话详情。
     * 归属校验：只有会话参与方（发起用户或所属商户）才能查看。
     */
    @Operation(summary = "查询会话详情")
    @GetMapping("/{sessionId}")
    public ApiResponse<SessionResponse> detail(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long sessionId) {
        CurrentUser currentUser = tokenService.resolveCurrentUser(token);
        Session session = sessionAccess.validateAndGetSession(sessionId, currentUser);
        return ApiResponse.success(sessionService.toResponse(session));
    }
}
