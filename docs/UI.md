# 前端开发任务：迭代 1（登录注册）+ 迭代 2（商户/用户工作台）

## 1. 技术栈（严格按此执行）
- Vue 3 + Vite + Vue Router 4
- 状态管理：Pinia
- UI 组件库：Element Plus（自动按需导入）
- HTTP 请求：Axios（需封装）
- 后端 API：严格按照我提供的 OpenAPI 3.0.1 规范（见附件），不得自行编造接口路径或字段。

## 2. 登录响应数据结构（已知晓）
- 用户登录 (`/user/login`) 与商户登录 (`/commercialTenant/login`) 响应 data 均为：
  `{ "id": number, "account": string, "name": string, "token": string }`
- 注册响应只返回 `{ id, name }`，注册成功后不自动登录，跳转回登录页。

## 3. 路由与页面结构
- **登录页**：路径 `/login`
    - 使用 `el-tabs` 切换“用户登录”和“商户登录”。
    - 表单字段：账号（`account`）、密码（`password`）。
    - 登录成功：存储 token 和用户信息到 Pinia + localStorage。
        - 商户 → 跳转 `/merchant/goods`
        - 用户 → 跳转 `/user/goods`

- **商户端布局**：路径 `/merchant`
    - 左侧侧边栏（`el-menu`）：
        - 菜单项来自 `/workbench/menu` 接口（返回商品管理/知识库/AI设置/会话收件箱/经营数据）。
        - 默认高亮“商品管理”。
        - 除“商品管理”外，其他菜单点击显示 `ElMessage.info('功能开发中')`，不跳转。
    - 主内容区 router-view 渲染 `/merchant/goods`。

- **用户端布局**：路径 `/user`
    - 左侧侧边栏（`el-menu`）：
        - **菜单1：商品**（路径 `/user/goods`，默认选中）
        - **菜单2：咨询**（点击无跳转，仅显示 `ElMessage.info('功能开发中')`）
    - 主内容区 router-view 渲染 `/user/goods`。

## 4. 商户端 - 商品管理（/merchant/goods）
- **顶部**：标题“商品管理” + “新增商品”按钮（`el-button`）。
- **表格**（`el-table`）：展示 ID、商品名称、创建时间、操作。
- **操作列**：编辑（`el-button` text）、删除（`el-button` text danger）。
- **新增/编辑**：使用 `el-dialog` 弹窗，仅包含 `name` 输入框。
- **删除**：使用 `ElMessageBox.confirm` 二次确认。
- **接口调用**：
    - 列表：`GET /goods/mine`（需携带 token，分页参数 pageNum/pageSize）。
    - 新增：`POST /goods/add`（需携带 token，body: `{ name }`）。
    - 更新：`PUT /goods/update`（需携带 token，body: `{ id, name }`）。
    - 删除：`DELETE /goods/delete?id=xxx`（需携带 token）。

## 5. 用户端 - 商品浏览（/user/goods）
- **展示风格**：iPhone 首页卡片式布局（圆角矩形框，类似应用图标）。
- **布局**：使用 `el-row` 和 `el-col`（如 `:span="6"` 或 `:span="8"`）自适应展示卡片。
- **卡片内容**（`el-card`，`body-style` 设置内边距）：
    - 中央大号显示商品名称（`item.name`）。
    - 卡片底部放置“咨询”按钮（`el-button` type="primary" plain 圆角）。
- **数据来源**：`GET /goods/all`（无需 token 也能访问）。
- **按钮交互**：
    - 点击“咨询”：若未登录（无 token），提示 `ElMessage.warning('请先登录')`。
    - 若已登录，提示 `ElMessage.info('咨询功能开发中')`。

## 6. Axios 封装与拦截器（关键）
- **BaseURL**：`http://localhost:8080`
- **请求拦截器**：从 Pinia store 读取 token，添加到请求头 `Authorization: ${token}`（注意：直接传 token 字符串，不加 `Bearer ` 前缀，因为后端 OpenAPI 定义的是纯字符串）。
- **响应拦截器**：
    - 若返回 `code === 0`，正常返回 `res.data`。
    - 若 `code !== 0`，使用 `ElMessage.error(res.message)` 提示错误。
    - 若 `code === 10401`（UNAUTHORIZED）或后端返回 401，清除 localStorage 和 Pinia 中的 token，跳转 `/login`。

## 7. 额外约束
- 商品列表必须支持分页（`el-pagination` 放在表格下方）。
- 所有请求 loading 状态由 `ElLoading` 或 `v-loading` 控制。
- 代码风格使用 Vue 3 `<script setup>` 组合式 API。

---

# 前端 UI 增量更新任务：迭代3（用户端选品 + 纯人工客服实时双端通讯）

## 当前状态
- 迭代1+2 的 UI 已完成：
    - 登录页（用户/商户 Tab 切换）
    - 商户端布局 + 商品管理 CRUD（/merchant/goods）
    - 用户端布局 + 商品卡片浏览（/user/goods）
    - Axios 拦截器、Pinia 状态管理已就绪
- **本次是增量更新**，不破坏已有功能。

## 技术栈（不变）
- Vue 3 + Vite + Vue Router 4 + Pinia + Element Plus
- WebSocket：原生 WebSocket API（封装为可复用服务）


## 一、命名与菜单变更（重要）

### 1.1 用户端菜单改名

| 原菜单名 | 新菜单名 | 路径 | 状态 |
|---|---|---|---|
| 商品 | 商品 | `/user/goods` | 保持不变 |
| 咨询 | **我的咨询** | `/user/chat` | 改为可用（迭代3），不再弹"功能开发中" |

### 1.2 商户端菜单改可用

| 菜单名 | 路径 | 状态 |
|---|---|---|
| 商品管理 | `/merchant/goods` | 保持不变 |
| 知识库 | — | 保持占位（后续迭代） |
| AI设置 | — | 保持占位（后续迭代） |
| **会话收件箱** | `/merchant/chat` | **改为可用（迭代3）**，不再弹"功能开发中" |
| 经营数据 | — | 保持占位（后续迭代） |

**红点要求**：商户端侧边栏"会话收件箱"菜单旁显示未读消息总数角标（从 `/session/ct/list` 各会话 `unreadCount` 求和），>0 时显示红点或数字。


## 二、路由新增

| 路径 | 组件 | 说明 |
|---|---|---|
| `/user/chat` | 用户端"我的咨询"列表页 | 展示当前用户的所有会话 |
| `/user/chat/:sessionId?` | 用户端聊天窗口 | 与商户对话，`sessionId` 可选（新建会话时不带） |
| `/merchant/chat` | 商户端"会话收件箱"列表页 | 展示该商户的所有会话 |
| `/merchant/chat/:sessionId` | 商户端聊天窗口 | 回复用户，`sessionId` 必传 |


## 三、用户端变更

### 3.1 商品卡片"咨询"按钮交互变更

当前"咨询"按钮只弹 `ElMessage.info('功能开发中')`，现改为：

- **已登录**：点击 → 跳转 `/user/chat?goodsId=xxx&ctId=xxx`
- **未登录**：弹 `ElMessage.warning('请先登录')` → 跳转 `/login`

### 3.2 "我的咨询"列表页（/user/chat）

**接口**：`GET /session/user/list`（需携带 token，分页参数 pageNum/pageSize，pageSize 默认 10）

**列表项展示**：
- 商户名称（从会话数据中取，暂缺则显示"商户"）
- 商品名称（从会话数据中取，暂缺则显示"商品"）
- 最后一条消息预览（`lastMessageContent`，超过 50 字截断加 `...`）
- 最后消息时间（`lastMessageTime`，做相对时间格式化："3分钟前""昨天 14:30"）
- **未读红点**：`unreadCount > 0` 时显示红点或数字角标（`el-badge`）

**交互**：点击任意会话 → 跳转 `/user/chat/:sessionId`

### 3.3 用户端聊天窗口（/user/chat/:sessionId?）

**布局**：顶部标题栏 + 消息列表 + 底部输入框

**顶部标题栏**：显示对话对象名称（商户名或商品名）

**进入页面时的逻辑**：

1. 建立 WebSocket 连接：`ws://localhost:8080/user/chat/{userId}?token={userToken}`
2. **如果 URL 中有 `sessionId`**（从"我的咨询"列表点击进入）：
    - 调用 `GET /session/{sessionId}/message/list` 加载历史消息（分页，按 `createdAt` 升序，pageSize 默认 50）
    - 调用 `PUT /session/{sessionId}/message/read` 标记该会话中商户发给用户的消息为已读
3. **如果 URL 中无 `sessionId`**（从商品卡片"咨询"按钮跳转进入）：
    - 不加载历史消息
    - 页面 URL 中携带 `goodsId` 和 `ctId` 参数（如 `/user/chat?goodsId=1&ctId=1`）

**消息列表渲染**：
- 区分"我发的"和"对方发的"：气泡颜色不同（我是绿色/对方灰色）
- 按 `createdAt` 升序排列，自动滚动到最新消息
- 发送中的消息可显示临时状态（可选）

**发送消息**：
- 通过 WebSocket 发送，输入框支持回车发送
- **新建会话（URL 中无 sessionId）**：
  ```json
  { "goodsId": 1, "ctId": 1, "message": "你好" }
  ```
- **已有会话（URL 中有 sessionId）**：
  ```json
  { "sessionId": 123, "message": "你好" }
  ```

**接收消息处理**：

| 收到的 state | 含义 | 前端处理 |
|---|---|---|
| `SESSION_CREATED` | 会话新建成功 | 记录 `sessionId`（更新 URL），消息追加到列表 |
| `SUCCESS` | 消息发送成功 | 消息追加到列表 |
| `ERROR` | 消息发送失败 | `ElMessage.error('发送失败，请重试')` |
| 收到对方消息 | `senderType !== 当前用户类型` | 实时追加到消息列表 |


## 四、商户端变更

### 4.1 会话收件箱列表页（/merchant/chat）

**接口**：`GET /session/ct/list`（需携带 token，分页参数 pageNum/pageSize，pageSize 默认 10）

**列表项展示**（同用户端）：
- 用户名称（从会话数据中取，暂缺则显示"用户"）
- 商品名称（从会话数据中取，暂缺则显示"商品"）
- 最后一条消息预览（`lastMessageContent`，超过 50 字截断）
- 最后消息时间（`lastMessageTime`，相对时间格式化）
- **未读红点**：`unreadCount > 0` 时显示红点或数字角标

**交互**：点击任意会话 → 跳转 `/merchant/chat/:sessionId`

### 4.2 商户端聊天窗口（/merchant/chat/:sessionId）

**布局**：同用户端聊天窗口

**进入页面时的逻辑**：

1. 建立 WebSocket 连接：`ws://localhost:8080/commercialTenant/chat/{ctId}?token={tenantToken}`
2. 调用 `GET /session/{sessionId}/message/list` 加载历史消息
3. 调用 `PUT /session/{sessionId}/message/read` 标记该会话中用户发给商户的消息为已读

**发送消息**：通过 WebSocket 发送，格式同用户端（**必须携带 `sessionId`**）：
```json
{ "sessionId": 123, "message": "您好，有什么可以帮您" }
```


## 五、WebSocket 通用机制（两端共用）

### 5.1 相对时间格式化规则

使用 `dayjs` 库，统一格式：
- 1分钟内：`"刚刚"`
- 1小时内：`"X分钟前"`
- 24小时内：`"X小时前"`
- 超过24小时且在同一年：`"MM-DD HH:mm"`
- 跨年：`"YYYY-MM-DD HH:mm"`

### 5.2 连接建立
- 组件挂载时建立连接
- 连接 URL 中携带 token（从 Pinia 获取）
- 连接成功后才能发送消息
- 连接建立后，根据 URL 参数决定是否加载历史消息

### 5.3 断线重连 + 增量消息补齐（关键）
- 指数退避策略，初始间隔 1s，最多重试 5 次
- **重连成功后，保持本地已加载的消息不变**（不清除、不重新渲染）
- 调用 `GET /session/{sessionId}/message/list` 拉取增量消息
- **拉取起点**：本地消息列表中最后一条消息的 `createdAt`
- 服务端返回该时间点之后的新消息，前端**拼接在消息列表底部**
- 如果本地没有消息（首次进入），正常拉取全部历史消息
- 拼接后自动滚动到最新消息

### 5.4 连接关闭
- 组件卸载时主动关闭 WebSocket 连接
- 页面刷新/关闭时，浏览器会自动关闭 WS 连接

### 5.5 关闭码 4001 处理（被顶替）
- 收到 `onclose` 且 `event.code === 4001` 时：
    - `ElMessage.warning('该账号已在别处登录')`
    - **不触发自动重连**
    - 清除本地 token，跳转登录页 `/login`

### 5.6 其他关闭码处理
- 非 4001 的正常关闭：按断线重连逻辑处理（最多 5 次）


## 六、组件拆分建议

| 组件名 | 用途 | 复用场景 |
|---|---|---|
| `SessionList.vue` | 会话列表（分页 + 预览 + 未读红点） | 用户端 `/user/chat` 和商户端 `/merchant/chat` |
| `ChatWindow.vue` | 聊天窗口（消息列表 + 输入框 + WS 管理） | 用户端和商户端，通过 props 区分类型 |
| `RelativeTime.vue` | 相对时间格式化 | 全局复用 |


## 七、额外约束

1. Token 区分：商户端用 `tenantToken`，用户端用 `userToken`，Pinia 中分开存储
2. 响应拦截器已处理 `code === 10401` → 清除 token 跳转登录（已有，无需改动）
3. 消息列表每次加载历史消息后，**只拼接增量**，不重复渲染已有消息
4. 所有会话相关接口均需携带 token（已在 Axios 拦截器中统一处理）
5. WebSocket 连接 URL 中的 `userId`/`ctId` 和 `token` 必须匹配，否则后端会拒绝连接（迭代3后端约束）


## 八、验收清单

### 菜单与路由
- [ ] 用户端"咨询"菜单改名为"我的咨询"，点击跳转 `/user/chat`
- [ ] 商户端"会话收件箱"菜单可用，点击跳转 `/merchant/chat`
- [ ] 商户端侧边栏"会话收件箱"显示未读总数角标

### 商品卡片咨询入口
- [ ] 已登录用户点击"咨询" → 跳转 `/user/chat?goodsId=xxx&ctId=xxx`
- [ ] 未登录用户点击"咨询" → 弹 `ElMessage.warning('请先登录')` → 跳转 `/login`

### 用户端"我的咨询"列表
- [ ] 列表展示所有会话，按 `lastMessageTime` 倒序
- [ ] 每个会话显示：商户名、商品名、最后消息预览、时间、未读红点
- [ ] 点击会话 → 跳转 `/user/chat/:sessionId`

### 用户端聊天窗口
- [ ] 从"我的咨询"进入：加载历史消息，标记已读
- [ ] 从商品卡片进入：不加载历史，发送首条消息后收到 `SESSION_CREATED`
- [ ] 消息列表区分"我发的"（绿色）和"对方发的"（灰色）
- [ ] 输入框支持回车发送
- [ ] 收到 `state=ERROR` 时显示错误提示

### 商户端会话收件箱
- [ ] 列表展示所有会话，按 `lastMessageTime` 倒序
- [ ] 每个会话显示：用户名、商品名、最后消息预览、时间、未读红点
- [ ] 点击会话 → 跳转 `/merchant/chat/:sessionId`

### 商户端聊天窗口
- [ ] 加载历史消息，标记已读
- [ ] 发送消息必须携带 `sessionId`
- [ ] 消息列表区分"我发的"和"对方发的"

### WebSocket 机制
- [ ] 断线后自动重连（指数退避，最多 5 次）
- [ ] 重连成功后，本地消息保留，增量消息拼接在底部
- [ ] 关闭码 4001 正确处理：弹提示，不自动重连，跳转登录
- [ ] 页面卸载时主动关闭 WS 连接