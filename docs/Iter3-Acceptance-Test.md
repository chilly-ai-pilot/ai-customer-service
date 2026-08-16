根据验收要求逐项说明验证方式：

---

## 验收命令速查

### 1. 商品浏览公开可访问
```bash
curl -s "http://localhost:8080/goods/all?pageNum=1&pageSize=10"
```
→ 返回商品列表，**无 Authorization 头也能访问**

---

### 2. 未登录无法建立用户 WS 连接
```bash
# 无 token
wscat -n ws://localhost:8080/user/chat/1

# 伪造 token
wscat -n "ws://localhost:8080/user/chat/1?token=fake_xyz"
```
→ 断言：握手直接失败，连接无法建立

---

### 3. 用户发首条消息建立会话
```bash
# WS 连接 ws://localhost:8080/user/chat/{userId}?token=user_token_1
# 发送：
{
  "goodsId": 1,
  "ctId": 1,
  "content": "你好"
}
```
→ 断言：收到 `state=SESSION_CREATED`，响应中含 `sessionId`

---

### 4. 归属信息不可篡改
```bash
# 后续发消息时故意换 goodsId：
{
  "sessionId": "上一步的sessionId",
  "goodsId": 999,
  "content": "测试篡改"
}
```
→ 断言：消息依然落入原 session，goodsId 以创建时为准，篡改字段不生效

---

### 5. 商户实时收到并回复
```bash
# 商户连接 ws://localhost:8080/commercialTenant/chat/{ctId}?token=token_A
# 断言：收到 state=DELIVERED 的消息推送
# 商户回复：
{
  "sessionId": "sessionId",
  "content": "您好，请问有什么可以帮您"
}
```
→ 断言：用户端收到 state=DELIVERED 推送

---

### 6. 商户离线时消息不丢、可查
```bash
# 不建立商户 WS 连接，直接用 REST 查消息列表
curl -s "http://localhost:8080/session/{sessionId}/message/list" \
  -H "Authorization: user_token_1"
```
→ 断言：消息已在 MySQL 中，查询返回完整内容，不报 500

---

### 7. 历史记录双向可查
```bash
# 商户查会话列表
curl -s "http://localhost:8080/session/ct/list" \
  -H "Authorization: token_A"

# 用户查会话列表
curl -s "http://localhost:8080/session/user/list" \
  -H "Authorization: user_token_1"
```
→ 断言：双方都能看到自己的会话列表，userId/ctId 从 token 解析，不走 query 参数

---

### 8. 冒充他人身份连接被拒绝
```bash
# 用商户 A 的 token，连接 ws://localhost:8080/commercialTenant/chat/{ctId_B}
wscat -n "ws://localhost:8080/commercialTenant/chat/999?token=token_A"
```
→ 断言：握手阶段拒绝，`onOpen` 不触发

---

### 9. 历史查询越权返回 FORBIDDEN
```bash
# 商户 A 的 token 查商户 B 的会话
curl -s "http://localhost:8080/session/ct/list" \
  -H "Authorization: token_B"
```
→ 断言：只返回 token_B 自己名下的会话，不会出现其他商户的数据

---

### 10. 消息接口越权查返回 403
```bash
curl -s "http://localhost:8080/session/1/message/list" \
  -H "Authorization: token_B"

curl -s "http://localhost:8080/session/1/message/unread-count" \
  -H "Authorization: token_B"
```
→ 断言：两接口均返回 `{"code": 403, ...}`，**不是 404**（session 存在但无权访问）

---

### 11. 重复咨询复用同一会话
```bash
# 第一次发消息（无 sessionId），得到 sessionId_1
# 断开重连，再发消息（无 sessionId，same goodsId+ctId）
```
→ 断言：第二次返回 `state=SUCCESS`，sessionId 与第一次相同，未新建

---

### 12. 重复登录使旧 WS 连接失效
```bash
# 用户已用 user_token_1 建 WS 连接
# 该账号再次登录，user_token_1 被顶替
# 等待约 30 秒（token 定时任务一轮）
```
→ 断言：旧连接收到关闭码 **4001**，前端 `onclose` 读到该码，提示"已在别处登录"

---

### 13. 落库失败时回 ERROR
```bash
# 模拟：WS 连接正常建立，发送消息时主动关掉 MySQL
```
→ 断言：发送方收到 `state=ERROR` 的响应，不是静默丢失

---

### 14. 同一身份重复建连接，旧连接被关闭
```bash
# 同一 token 两次建立 WS 连接
```
→ 断言：第二条连上，第一条收到关闭码 **4001**，不再接收任何消息

---

### 15. 心跳判活，死连接被清理
```bash
# 建立 WS 连接后，断开客户端网络（不关 Socket），等待约 60 秒
```
→ 断言：服务端主动关闭死连接，连接池中该条目被清理

---

### 16. 长时间不发消息，连接保持
```bash
# 建连后正常响应心跳，等待超过 5 分钟
```
→ 断言：连接存活，**未被以"不活跃"为由断开**

---

## 核心验证点速记

| 验证项 | 关键词 | 预期结果 |
|---|---|---|
| 商品公开 | 无头访问 `/goods/all` | 正常返回 |
| 无 token 建连 | 无/假 token | 握手失败 |
| 首条消息 | WS 发消息 | `SESSION_CREATED` |
| 归属不可改 | 篡改 goodsId | 以创建时为准 |
| 商户离线不丢消息 | 不建商户 WS，直接 REST 查 | 消息已在库 |
| 历史双向可查 | 商户+用户各自查列表 | 均能看到 |
| 冒充身份拒绝 | A 的 token 连 B 的路径 | 握手失败 |
| 越权查消息 | B 的 token 查 A 的 session | **403** |
| 会话复用 | 同 goodsId+ctId 发两次 | 同 sessionId |
| 重复登录顶替 | 旧 token 建连，新登录 | 旧连收到 **4001** |
| 重复建连 | 同一 token 两次 WS | 旧连收到 **4001** |
| 心跳清理死连 | 断网不断 Socket | 60s 后连接被关 |
| 长不活跃不断连 | 建连后只发心跳 | 连接保持存活 |

验收 10（越权 403）和验收 1（无头访问商品）是最容易出错的两个边界，建议优先验证。