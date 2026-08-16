package com.aicustomer.websocket;

import com.aicustomer.constant.ErrorCodes;
import com.aicustomer.constant.SubjectType;
import com.aicustomer.dto.request.LoginRequest;
import com.aicustomer.dto.request.RegisterRequest;
import com.aicustomer.entity.Goods;
import com.aicustomer.entity.Message;
import com.aicustomer.entity.Session;
import com.aicustomer.repository.GoodsRepository;
import com.aicustomer.repository.MessageRepository;
import com.aicustomer.repository.SessionRepository;
import com.aicustomer.service.TokenService;
import com.aicustomer.websocket.message.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class Iteration3WebSocketTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private MessageRepository messageRepository;

    private String tenantToken;
    private String userToken;
    private Long tenantId;
    private Long userId;
    private Long goodsId;

    private String registerAndLoginTenant(String account) throws Exception {
        RegisterRequest reg = new RegisterRequest(account, "pass123", "商户-" + account);
        MvcResult r = mockMvc.perform(post("/commercialTenant/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long tid = objectMapper.readTree(r.getResponse().getContentAsString()).at("/data/id").asLong();
        this.tenantId = tid;

        LoginRequest login = new LoginRequest(account, "pass123");
        MvcResult lr = mockMvc.perform(post("/commercialTenant/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(lr.getResponse().getContentAsString()).at("/data/token").asText();
    }

    private String registerAndLoginUser(String account) throws Exception {
        RegisterRequest reg = new RegisterRequest(account, "pass123", "用户-" + account);
        MvcResult r = mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long uid = objectMapper.readTree(r.getResponse().getContentAsString()).at("/data/id").asLong();
        this.userId = uid;

        LoginRequest login = new LoginRequest(account, "pass123");
        MvcResult lr = mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(lr.getResponse().getContentAsString()).at("/data/token").asText();
    }

    @BeforeEach
    void setUp() throws Exception {
        messageRepository.deleteAll();
        sessionRepository.deleteAll();
        goodsRepository.deleteAll();

        userToken = registerAndLoginUser("user_ws");
        tenantToken = registerAndLoginTenant("tenant_ws");

        Goods goods = new Goods();
        goods.setName("测试商品");
        goods.setCtId(tenantId);
        goodsId = goodsRepository.save(goods).getId();
    }

    // ==================== Token 解析测试 ====================

    @Nested
    @DisplayName("Token 解析 - 握手前置校验")
    class TokenResolveTests {

        @Test
        @DisplayName("T1 前置 - 有效 token 解析出正确 subjectId")
        void validToken_ResolvesCorrectSubjectId() {
            Long resolvedUserId = tokenService.resolve(SubjectType.USER, userToken);
            org.junit.jupiter.api.Assertions.assertEquals(userId, resolvedUserId);

            Long resolvedTenantId = tokenService.resolve(SubjectType.TENANT, tenantToken);
            org.junit.jupiter.api.Assertions.assertEquals(tenantId, resolvedTenantId);
        }

        @Test
        @DisplayName("T2 前置 - 路径身份不一致时，错误类型解析返回 null")
        void wrongTypeToken_ReturnsNull() {
            // USER token 用 TENANT 类型解析 → null
            Long resolvedAsTenant = tokenService.resolve(SubjectType.TENANT, userToken);
            org.junit.jupiter.api.Assertions.assertNull(resolvedAsTenant);

            // TENANT token 用 USER 类型解析 → null
            Long resolvedAsUser = tokenService.resolve(SubjectType.USER, tenantToken);
            org.junit.jupiter.api.Assertions.assertNull(resolvedAsUser);
        }

        @Test
        @DisplayName("T3 前置 - 无效 token 解析返回 null")
        void invalidToken_ReturnsNull() {
            Long resolved = tokenService.resolve(SubjectType.USER, "nonexistent_token_xyz");
            org.junit.jupiter.api.Assertions.assertNull(resolved);
        }
    }

    // ==================== 会话管理测试 ====================

    @Nested
    @DisplayName("会话管理 - T8/T9/T10")
    class SessionManagementTests {

        @Test
        @DisplayName("T8 - 商户查询自己名下的会话列表（分页）")
        void tenantListSessions_Pagination() throws Exception {
            Session session = Session.builder()
                    .userId(userId).ctId(tenantId).goodsId(goodsId)
                    .conversationStatus(Session.ConversationStatus.ACTIVE)
                    .lastMessageTime(Instant.now())
                    .build();
            sessionRepository.save(session);

            mockMvc.perform(get("/session/ct/list")
                            .header("Authorization", tenantToken)
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].ctId").value(tenantId))
                    .andExpect(jsonPath("$.data.content[0].userId").value(userId));
        }

        @Test
        @DisplayName("T9 - 用户查询自己的会话列表（分页）")
        void userListSessions_Pagination() throws Exception {
            Session session = Session.builder()
                    .userId(userId).ctId(tenantId).goodsId(goodsId)
                    .conversationStatus(Session.ConversationStatus.ACTIVE)
                    .lastMessageTime(Instant.now())
                    .build();
            sessionRepository.save(session);

            mockMvc.perform(get("/session/user/list")
                            .header("Authorization", userToken)
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].userId").value(userId));
        }

        @Test
        @DisplayName("T10 - 非会话参与方查询消息列表 → 403")
        void queryMessages_NotParticipant_Forbidden() throws Exception {
            // 注册另一个商户
            String otherToken = registerAndLoginTenant("other_tenant");

            Session session = Session.builder()
                    .userId(userId).ctId(tenantId).goodsId(goodsId)
                    .conversationStatus(Session.ConversationStatus.ACTIVE)
                    .lastMessageTime(Instant.now())
                    .build();
            Long sessionId = sessionRepository.save(session).getId();

            mockMvc.perform(get("/session/" + sessionId + "/message/list")
                            .header("Authorization", otherToken)
                            .param("pageNum", "1")
                            .param("pageSize", "50"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCodes.FORBIDDEN));
        }

        @Test
        @DisplayName("无 token 查询会话列表 → 401")
        void listSessions_NoToken_Unauthorized() throws Exception {
            mockMvc.perform(get("/session/ct/list")
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCodes.UNAUTHORIZED));

            mockMvc.perform(get("/session/user/list")
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCodes.UNAUTHORIZED));
        }

        @Test
        @DisplayName("用户 token 查询商户会话列表 → 401")
        void tenantList_UserToken_Unauthorized() throws Exception {
            mockMvc.perform(get("/session/ct/list")
                            .header("Authorization", userToken)
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCodes.UNAUTHORIZED));
        }
    }

    // ==================== 消息列表/已读/未读测试 ====================

    @Nested
    @DisplayName("消息操作 - T13/T14/T15")
    class MessageTests {

        private Long sessionId;

        @BeforeEach
        void createSession() {
            Session session = Session.builder()
                    .userId(userId).ctId(tenantId).goodsId(goodsId)
                    .conversationStatus(Session.ConversationStatus.ACTIVE)
                    .lastMessageTime(Instant.now())
                    .build();
            sessionId = sessionRepository.save(session).getId();
        }

        @Test
        @DisplayName("T13 - 查询消息列表（按时间正序）")
        void queryMessageList_TimeAscending() throws Exception {
            mockMvc.perform(get("/session/" + sessionId + "/message/list")
                            .header("Authorization", userToken)
                            .param("pageNum", "1")
                            .param("pageSize", "50"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.content", hasSize(0)));
        }

        @Test
        @DisplayName("T14 - 标记已读（单向，精确到接收方）")
        void markRead_SingleDirection() throws Exception {
            // USER 作为接收方标记已读
            mockMvc.perform(put("/session/" + sessionId + "/message/read")
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                    .andExpect(jsonPath("$.data.markedCount").value(0));

            // TENANT 作为接收方标记已读
            mockMvc.perform(put("/session/" + sessionId + "/message/read")
                            .header("Authorization", tenantToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.markedCount").value(0));
        }

        @Test
        @DisplayName("T15 - 未读数统计")
        void unreadCount_ReturnsCorrectNumber() throws Exception {
            mockMvc.perform(get("/session/" + sessionId + "/message/unread-count")
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                    .andExpect(jsonPath("$.data.unreadCount").value(0));
        }

        @Test
        @DisplayName("T15 变体 - 有未读消息时未读数正确")
        void unreadCount_WithUnreadMessages() throws Exception {
            // 商户发一条消息给用户，用户端未读数应为1
            Message msg = Message.builder()
                    .messageId("msg_unread_1")
                    .sessionId(sessionId)
                    .senderId(tenantId).senderType(SubjectType.TENANT)
                    .receiverId(userId).receiverType(SubjectType.USER)
                    .content("您好，请问有什么需要帮助？")
                    .isRead(false)
                    .build();
            messageRepository.save(msg);

            // 用户查询未读数
            mockMvc.perform(get("/session/" + sessionId + "/message/unread-count")
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.unreadCount").value(1));

            // 商户查询自己的未读数为0
            mockMvc.perform(get("/session/" + sessionId + "/message/unread-count")
                            .header("Authorization", tenantToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.unreadCount").value(0));
        }

        @Test
        @DisplayName("T14 变体 - 标记已读后未读数归零")
        void markRead_ThenUnreadCountZero() throws Exception {
            // 商户发一条消息
            Message msg = Message.builder()
                    .messageId("msg_mark_read_1")
                    .sessionId(sessionId)
                    .senderId(tenantId).senderType(SubjectType.TENANT)
                    .receiverId(userId).receiverType(SubjectType.USER)
                    .content("您好，请问有什么需要帮助？")
                    .isRead(false)
                    .build();
            messageRepository.save(msg);

            // 初始未读数=1
            mockMvc.perform(get("/session/" + sessionId + "/message/unread-count")
                            .header("Authorization", userToken))
                    .andExpect(jsonPath("$.data.unreadCount").value(1));

            // 标记已读
            mockMvc.perform(put("/session/" + sessionId + "/message/read")
                            .header("Authorization", userToken))
                    .andExpect(jsonPath("$.data.markedCount").value(1));

            // 未读数归零
            mockMvc.perform(get("/session/" + sessionId + "/message/unread-count")
                            .header("Authorization", userToken))
                    .andExpect(jsonPath("$.data.unreadCount").value(0));
        }

        @Test
        @DisplayName("非参与方标记已读 → 403")
        void markRead_NotParticipant_Forbidden() throws Exception {
            String otherUserToken = registerAndLoginUser("other_user_markread");

            mockMvc.perform(put("/session/" + sessionId + "/message/read")
                            .header("Authorization", otherUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCodes.FORBIDDEN));
        }

        @Test
        @DisplayName("非参与方查询未读数 → 403")
        void unreadCount_NotParticipant_Forbidden() throws Exception {
            String otherUserToken = registerAndLoginUser("other_user_unread");

            mockMvc.perform(get("/session/" + sessionId + "/message/unread-count")
                            .header("Authorization", otherUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCodes.FORBIDDEN));
        }
    }

    // ==================== Session 自动创建测试 ====================

    @Nested
    @DisplayName("Session 自动创建 - T6/T7")
    class SessionAutoCreationTests {

        @Autowired
        private ChatService chatService;

        @Test
        @DisplayName("T6 - 用户发首条消息，session不存在 → 自动创建")
        void firstMessage_AutoCreateSession() {
            // 确认 session 不存在
            org.junit.jupiter.api.Assertions.assertTrue(
                    sessionRepository.findByUserIdAndCtIdAndGoodsId(userId, tenantId, goodsId).isEmpty());

            // 模拟用户发消息
            ChatMessage request = ChatMessage.builder()
                    .goodsId(goodsId)
                    .ctId(tenantId)
                    .content("你好，我想咨询一下这个商品")
                    .build();

            ChatMessage response = chatService.handleUserMessage(userId, request);

            // 验证 SESSION_CREATED 响应
            org.junit.jupiter.api.Assertions.assertEquals(ChatMessage.State.SESSION_CREATED, response.getState());
            org.junit.jupiter.api.Assertions.assertNotNull(response.getSessionId());

            // 验证 session 已创建
            org.junit.jupiter.api.Assertions.assertTrue(
                    sessionRepository.findById(response.getSessionId()).isPresent());
        }

        @Test
        @DisplayName("T7 - 用户发消息，session已存在 → 复用，返回 SUCCESS")
        void messageWithExistingSession_Reuse() {
            // 先创建一个 session
            Session existing = Session.builder()
                    .userId(userId).ctId(tenantId).goodsId(goodsId)
                    .conversationStatus(Session.ConversationStatus.ACTIVE)
                    .lastMessageTime(Instant.now())
                    .build();
            Long existingSessionId = sessionRepository.save(existing).getId();

            // 发消息（带 sessionId）
            ChatMessage request = ChatMessage.builder()
                    .sessionId(existingSessionId)
                    .content("继续咨询")
                    .build();

            ChatMessage response = chatService.handleUserMessage(userId, request);

            // 验证 SUCCESS 响应，不是 SESSION_CREATED
            org.junit.jupiter.api.Assertions.assertEquals(ChatMessage.State.SUCCESS, response.getState());
            org.junit.jupiter.api.Assertions.assertEquals(existingSessionId, response.getSessionId());

            // 验证 session 数量仍然是1（没有创建新的）
            long count = sessionRepository.findByUserIdAndCtIdAndGoodsId(userId, tenantId, goodsId)
                    .map(s -> 1L).orElse(0L);
            org.junit.jupiter.api.Assertions.assertEquals(1L, count);
        }

        @Test
        @DisplayName("T12 变体 - 商户发消息，消息落库并可查询")
        void tenantSendMessage_PersistedAndQueryable() {
            Session session = Session.builder()
                    .userId(userId).ctId(tenantId).goodsId(goodsId)
                    .conversationStatus(Session.ConversationStatus.ACTIVE)
                    .lastMessageTime(Instant.now())
                    .build();
            Long sessionId = sessionRepository.save(session).getId();

            ChatMessage request = ChatMessage.builder()
                    .sessionId(sessionId)
                    .content("您好，请问有什么需要帮助？")
                    .build();

            ChatMessage response = chatService.handleTenantMessage(tenantId, request);

            org.junit.jupiter.api.Assertions.assertEquals(ChatMessage.State.SUCCESS, response.getState());
            org.junit.jupiter.api.Assertions.assertNotNull(response.getMessageId());

            // 验证消息已落库
            var messages = messageRepository.findByMessageIdIn(java.util.List.of(response.getMessageId()));
            org.junit.jupiter.api.Assertions.assertEquals(1, messages.size());
            Message saved = messages.get(0);
            org.junit.jupiter.api.Assertions.assertEquals(tenantId, saved.getSenderId());
            org.junit.jupiter.api.Assertions.assertEquals(SubjectType.TENANT, saved.getSenderType());
            org.junit.jupiter.api.Assertions.assertEquals(userId, saved.getReceiverId());
            org.junit.jupiter.api.Assertions.assertEquals(SubjectType.USER, saved.getReceiverType());
            org.junit.jupiter.api.Assertions.assertEquals("您好，请问有什么需要帮助？", saved.getContent());
        }

        @Test
        @DisplayName("T11 变体 - 用户发消息，消息落库并可查询")
        void userSendMessage_PersistedAndQueryable() {
            ChatMessage request = ChatMessage.builder()
                    .goodsId(goodsId)
                    .ctId(tenantId)
                    .content("你好，我想咨询一下这个商品")
                    .build();

            ChatMessage response = chatService.handleUserMessage(userId, request);

            org.junit.jupiter.api.Assertions.assertNotNull(response.getMessageId());

            var messages = messageRepository.findByMessageIdIn(java.util.List.of(response.getMessageId()));
            org.junit.jupiter.api.Assertions.assertEquals(1, messages.size());
            Message saved = messages.get(0);
            org.junit.jupiter.api.Assertions.assertEquals(userId, saved.getSenderId());
            org.junit.jupiter.api.Assertions.assertEquals(SubjectType.USER, saved.getSenderType());
            org.junit.jupiter.api.Assertions.assertEquals(tenantId, saved.getReceiverId());
            org.junit.jupiter.api.Assertions.assertEquals(SubjectType.TENANT, saved.getReceiverType());
        }

        @Test
        @DisplayName("非法的商户 session → ERROR")
        void tenantMessage_InvalidSession_Error() {
            ChatMessage request = ChatMessage.builder()
                    .sessionId(99999L)
                    .content("消息")
                    .build();

            ChatMessage response = chatService.handleTenantMessage(tenantId, request);
            org.junit.jupiter.api.Assertions.assertEquals(ChatMessage.State.ERROR, response.getState());
        }

        @Test
        @DisplayName("非该商户的 session → ERROR")
        void tenantMessage_WrongOwnerSession_Error() throws Exception {
            // 注册另一商户
            String otherTenantToken = registerAndLoginTenant("other_tenant_ct");

            // 为另一商户创建 session
            Session otherSession = Session.builder()
                    .userId(userId).ctId(tenantId).goodsId(goodsId)
                    .conversationStatus(Session.ConversationStatus.ACTIVE)
                    .lastMessageTime(Instant.now())
                    .build();
            Long sessionId = sessionRepository.save(otherSession).getId();

            // 获取新商户的 ID
            Long otherTenantId = tokenService.resolve(SubjectType.TENANT, otherTenantToken);

            ChatMessage request = ChatMessage.builder()
                    .sessionId(sessionId)
                    .content("消息")
                    .build();

            ChatMessage response = chatService.handleTenantMessage(otherTenantId, request);
            org.junit.jupiter.api.Assertions.assertEquals(ChatMessage.State.ERROR, response.getState());
        }
    }

    // ==================== REST 接口 - 会话查询 ====================

    @Nested
    @DisplayName("会话详情查询")
    class SessionDetailTests {

        @Test
        @DisplayName("会话参与方可以查询会话详情")
        void sessionDetail_Participant_CanQuery() throws Exception {
            Session session = Session.builder()
                    .userId(userId).ctId(tenantId).goodsId(goodsId)
                    .conversationStatus(Session.ConversationStatus.ACTIVE)
                    .lastMessageTime(Instant.now())
                    .build();
            Long sessionId = sessionRepository.save(session).getId();

            // 用户查询
            mockMvc.perform(get("/session/" + sessionId)
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.id").value(sessionId));

            // 商户查询
            mockMvc.perform(get("/session/" + sessionId)
                            .header("Authorization", tenantToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.id").value(sessionId));
        }

        @Test
        @DisplayName("非参与方查询会话详情 → 403")
        void sessionDetail_NotParticipant_Forbidden() throws Exception {
            String otherTenantToken = registerAndLoginTenant("other_tenant_detail");

            Session session = Session.builder()
                    .userId(userId).ctId(tenantId).goodsId(goodsId)
                    .conversationStatus(Session.ConversationStatus.ACTIVE)
                    .lastMessageTime(Instant.now())
                    .build();
            Long sessionId = sessionRepository.save(session).getId();

            mockMvc.perform(get("/session/" + sessionId)
                            .header("Authorization", otherTenantToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCodes.FORBIDDEN));
        }
    }
}
