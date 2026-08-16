# 前端 UI 规格文档

本文档记录前端的设计决策、实现细节和迭代历史，作为代码注释的补充说明。

---

## 迭代历程

### 迭代 1：登录注册

- 用户端/商户端 Tab 切换登录
- 注册弹窗
- Pinia + localStorage 存储 token

### 迭代 2：商户/用户工作台

- 商户端：商品管理 CRUD（表格 + 弹窗）
- 用户端：商品卡片浏览

### 迭代 3：用户端选品 + 纯人工客服实时双端通讯

- 用户端/商户端双端独立登录态（localStorage 前缀隔离）
- 聊天会话（WebSocket + REST 历史消息）
- 会话列表（未读红点）
- 商户端"会话收件箱"菜单变为可用

---

## 一、技术栈

- Vue 3（`<script setup>` 组合式 API）
- Vue Router 4
- Pinia（双身份 Store）
- Element Plus（按需自动导入，图标手动全局注册）
- Axios（封装拦截器）
- WebSocket：原生 WebSocket API

---

## 二、登录与身份隔离

### 设计背景

早期设计中，token/userInfo/userType 存在固定的 localStorage key 下，用户号登录会覆盖商户号登录态，无法同时保持两个身份登录。

### 解决方案

用户端和商户端分别用不同的 localStorage 前缀存储：

```
用户端 → localStorage: user_token, user_userInfo, user_userType
商户端 → localStorage: merchant_token, merchant_userInfo, merchant_userType
```

三个 Store 工厂函数：

- `useUserAuthStore()` — 用户端专用，固定 userType='USER'
- `useMerchantAuthStore()` — 商户端专用，固定 userType='TENANT'
- `useAuthStore()` — 通用入口，根据 `router.currentRoute.value.path` 自动判断当前在哪个分支，返回对应的 Store

登录页（`/login`）不属于 `/user` 或 `/merchant` 任何一个分支，两个表单分别显式调用 `useUserAuthStore()` / `useMerchantAuthStore()`，不会触发自动判断逻辑。

---

## 三、路由设计

### 命名约定

- 前端路由统一使用 `/inbox`（而非 `/chat`），与迭代 4 起的实现保持一致
- WebSocket 连接地址是后端接口路径，不受此命名影响

### 用户端路由

```
/user/goods        — 商品浏览
/user/inbox         — 我的咨询（会话列表）
/user/inbox/:sessionId?  — 聊天窗口（sessionId 可选）
```

### 商户端路由

```
/merchant/goods            — 商品管理
/merchant/inbox            — 会话收件箱（会话列表）
/merchant/inbox/:sessionId — 聊天窗口
```

### sessionId 可选的设计

从商品卡片"咨询"进入时，URL 是 `/user/inbox?goodsId=1&ctId=1`，不带 sessionId（此时还没建立会话）。

同一命名路由（`UserChatWindow`），SESSION_CREATED 后用 `router.replace` 更新 params，把 sessionId 注入 URL：

```js
function handleSessionCreated(sessionId) {
  router.replace({ name: 'UserChatWindow', params: { sessionId } })
}
```

这样不会重新挂载组件，WebSocket 连接得以保留。

---

## 四、会话列表（SessionList 组件）

### 数据加载

1. 调用 `listByUser` 或 `listByTenant` 接口获取会话分页数据
2. 会话列表接口本身没有返回商品名称/商户名称/用户名称，只有 ID
3. 并行发起两批名称查询：
   - `GET /goods/detail?id=xxx` → 换取商品名称，缓存到 `goodsNameMap`
   - `GET /user/{id}/name` 或 `GET /commercialTenant/{id}/name` → 换取对方名称，缓存到 `partnerNameMap`
4. 名称解析不阻塞表格展示（Promise.all 并行执行）

### 名称缓存

- 使用 Map 存储，已查询过的 ID 不再重复请求
- 整体替换 Map 触发 Vue 响应式更新（Map 直接赋值不触发）
- 查询失败时继续用编号兜底，不阻断列表渲染

### 全局事件驱动刷新

ChatWindow 标记消息已读后，派发自定义全局事件：

```js
window.dispatchEvent(new CustomEvent('chat:session-read', {
  detail: { sessionId: currentSessionId.value }
}))
```

SessionList 监听该事件，若被标记的会话在当前列表中且有未读数，则刷新列表。

---

## 五、聊天窗口（ChatWindow 组件）

### 历史消息加载

后端消息列表接口只支持 `pageNum`/`pageSize` 分页，没有"某时间点之后"的增量查询参数。

实现方式：

1. 先用 `pageSize=1` 探出 `totalElements`
2. 再用该总数一次性拉取全部消息（按 `createdAt` 升序）
3. 本地按 `id` 去重后追加到列表

> 局限性：会话消息量大时请求体会变大。后续若需支持长会话，需后端补充支持增量游标（如 `since`/`afterId` 参数）的接口。

### 断线重连时的增量合并

重连成功后，保持本地已加载的消息不动，只把本地最后一条消息之后的新消息追加进来：

1. 用 `pageSize=1` 探 `totalElements`，再一次性拉全部
2. 按本地最后一条消息的 `createdAt` 过滤
3. 按 `id` 去重
4. 追加并排序

### 待确认消息队列（FIFO）

WS 发送协议里没有客户端自定义字段，服务端回执里也只有 `messageId`（服务端生成）。

前端用 FIFO 队列解决"哪条本地气泡对应哪条回执"的问题：

1. 发送消息前，本地生成 `tempId`，入队
2. 收到 `SESSION_CREATED`/`SUCCESS`/`ERROR` 时，队首出队，找到对应气泡，更新状态

### 标题解析

聊天窗口顶部标题默认用父组件传的 title，等异步查到对方真实名字/商品名字后悄悄替换：

- 用户端：ctId 大多数从路由 query 直接拿到；从会话列表进入时 query 里没有 ctId，需查一次 `/session/{id}` 详情
- 商户端：永远从 sessionId 进入，只能查会话详情获取 userId
- 商品名：用户端从路由 query 拿，商户端从会话详情查

会话详情有缓存（`sessionDetailPromise`），同一 sessionId 只查一次。

### 新会话场景：复用历史会话

用户从商品卡片进入时，后端可能已有该用户对该商户该商品的历史会话。ChatWindow 挂载时主动查询用户会话列表，按 `ctId+goodsId` 找是否有既有会话，有则直接复用其 sessionId 加载历史，不用等发消息才建立会话。

---

## 六、WebSocket 连接管理（ws/chat.js）

### 连接状态

- `CONNECTING` / `OPEN`：忽略，不重复建立
- 其他状态：正常建立连接

### 重连策略

- 指数退避：1s → 2s → 4s → 8s → 16s
- 最多重试 5 次，达到后停止

### 关闭码处理

- `4001`：服务端通知"该账号已在别处登录"，不触发重连，清除登录态跳转登录页
- 其他关闭码：触发指数退避重连

### 心跳

服务端每 30 秒发原生 Ping 帧，浏览器自动回复 Pong 帧（对 JS 透明）。前端不需要任何心跳代码，只需要处理连接断开（`onclose`）。

---

## 七、相对时间格式化

使用 dayjs 库，规则如下：

| 时间范围 | 格式 |
|---------|------|
| < 1 分钟 | "刚刚" |
| < 60 分钟 | "X分钟前" |
| < 24 小时 | "X小时前" |
| 同一年 | "MM-DD HH:mm" |
| 跨年 | "YYYY-MM-DD HH:mm" |

RelativeTime 组件每 60 秒刷新一次，保证动态文本实时更新。
