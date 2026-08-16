

## 迭代 3 完整实现方案

---

### 第一步：数据层 ✅ 确认完毕，待写文件

| 文件 | 路径 | 用途 |
|---|---|---|
| `SubjectType` | `constant/SubjectType.java` | 已存在，无需新建 |
| `Session` 实体 | `entity/Session.java` | 会话记录，含 `conversationStatus`/`timestamp`、联合索引 |
| `SessionRepository` | `repository/SessionRepository.java` | 含 `findByUserIdAndCtIdAndGoodsId` |
| `ChatMessage` 消息体 | `websocket/message/ChatMessage.java` | WS 帧消息，含 `State` 枚举、`senderType` 用 `SubjectType` |

---

### 第二步：WS 端点 + 连接池（本批）

| 文件 | 路径 | 用途 |
|---|---|---|
| `ChatConnection` | `websocket/ChatConnection.java` | 连接上下文，含 Jakarta `Session`、`token`、`subjectId`、`subjectType` |
| `ConnectionPool` | `websocket/ConnectionPool.java` | 两个 `ConcurrentHashMap`（TENANT/USER），含旧连接关闭逻辑 |
| `ChatEndpointConfigurator` | `websocket/config/ChatEndpointConfigurator.java` | 握手阶段校验 token + 路径身份一致性，不一致拒绝 |
| `UserChatEndpoint` | `websocket/UserChatEndpoint.java` | `/user/chat/{userId}?token=xxx` 端点 |
| `CommercialTenantChatEndpoint` | `websocket/CommercialTenantChatEndpoint.java` | `/commercialTenant/chat/{ctId}?token=xxx` 端点 |

**核心逻辑**：
- `modifyHandshake`：解析 token → 查 TokenService → 比对路径身份，不一致抛异常拒绝握手
- `onOpen`：从池中取旧连接 → 发 `close(4001)` → 放入新连接
- `onMessage`：解析 `ChatMessage` → 无 sessionId 则走"首条消息建会话"逻辑（有历史则复用）→ 落库 → 路由转发另一端 → 回 `SUCCESS`/`SESSION_CREATED`/`ERROR`

---

### 第三步：消息落库

| 文件 | 路径 | 用途 |
|---|---|---|
| `Message` 实体 | `entity/Message.java` | 消息记录，含会话归属、发送方、已读标记 |
| `MessageRepository` | `repository/MessageRepository.java` | 含按 sessionId 查消息列表、按发送方/接收方查未读数 |

---

### 第四步：定时任务

| 文件 | 路径 | 用途 |
|---|---|---|
| `HeartbeatTask` | `websocket/task/HeartbeatTask.java` | 每 30 秒发原生 ping，累计 2 次无响应则 close(4001) + 清理池 |
| `TokenValidationTask` | `websocket/task/TokenValidationTask.java` | 每 30 秒遍历池中连接，查 TokenService，不在则 close(4001) + 清理 |
| `WebSocketTaskInitializer` | `websocket/task/WebSocketTaskInitializer.java` | 服务启动时注册两个定时任务 |

---

### 第五步：REST 接口

| 接口 | 方法 | 身份来源 | 归属校验 |
|---|---|---|---|
| `/session/user/list` | GET | userToken 解析 | — |
| `/session/ct/list` | GET | tenantToken 解析 | — |
| `/session/{sessionId}/message/list` | GET | token 解析 | 校验身份是 session 参与方 |
| `/session/{sessionId}/message/read` | PUT | token 解析 | 自动分流已读方向 |
| `/session/{sessionId}/message/unread-count` | GET | token 解析 | 校验身份是 session 参与方 |

---

### 第六步：确保 `/goods/all` 公开可访问

检查 `GoodsController`，移除该接口可能有的商户身份校验。

---
