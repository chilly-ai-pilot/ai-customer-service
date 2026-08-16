# AI 客服系统 - 前端

基于 Vue 3 的 AI 客服系统前端项目，使用 Vite 构建。

## 技术栈

- **Vue 3** — 渐进式 JavaScript 框架（`<script setup>` 组合式 API）
- **Vue Router 4** — 官方路由管理
- **Pinia** — 状态管理（支持用户端/商户端双身份独立登录态）
- **Element Plus** — UI 组件库（按需自动导入）
- **Axios** — HTTP 请求库（封装请求/响应拦截器）
- **Vite** — 下一代前端构建工具

## 项目结构

```
ui/
├── src/
│   ├── api/              # Axios 封装及接口分组
│   │   ├── index.js      # 接口导出（session/goods/user/commercialTenant/workbench）
│   │   └── request.js    # axios 实例、请求/响应拦截器
│   ├── components/       # 公共组件
│   │   ├── ChatWindow.vue     # 聊天窗口（消息列表 + 输入框 + WS 管理，用户/商户端复用）
│   │   ├── RelativeTime.vue   # 相对时间格式化（"3分钟前"等）
│   │   └── SessionList.vue    # 会话列表（分页 + 预览 + 未读红点，用户/商户端复用）
│   ├── router/
│   │   └── index.js      # 路由配置（/login, /merchant/*, /user/*）
│   ├── stores/
│   │   └── auth.js       # 双身份认证 Store（用户端/商户端独立存储，互不覆盖）
│   ├── utils/
│   │   └── dayjs.js      # dayjs 配置及相对时间格式化工具函数
│   ├── views/             # 页面视图
│   │   ├── LoginView.vue          # 登录页（用户/商户 Tab 切换 + 注册弹窗）
│   │   ├── UserLayout.vue         # 用户端布局（含侧边栏 + 顶栏）
│   │   ├── MerchantLayout.vue      # 商户端布局（含侧边栏 + 顶栏 + 未读数红点）
│   │   ├── UserChatView.vue       # 用户端"我的咨询"列表页
│   │   ├── UserChatWindowView.vue # 用户端聊天窗口
│   │   ├── UserGoodsView.vue      # 用户端商品浏览（卡片式）
│   │   ├── MerchantChatView.vue   # 商户端"会话收件箱"列表页
│   │   ├── MerchantChatWindowView.vue # 商户端聊天窗口
│   │   └── MerchantGoodsView.vue   # 商户端商品管理（CRUD）
│   ├── ws/
│   │   └── chat.js      # WebSocket 连接管理（连接/发送/重连/销毁）
│   ├── App.vue           # 根组件（提供 Element Plus 中文语言包）
│   └── main.js          # 入口文件
├── index.html
├── vite.config.js
└── package.json
```

## 功能模块

### 用户端（`/user`）

- **商品浏览** — 卡片式展示全部商品（无需登录）
- **我的咨询** — 会话列表（未读红点）+ 聊天窗口（实时 WS 通讯）
- **咨询入口** — 点击商品卡片"咨询"按钮，已登录跳转聊天窗口，未登录跳转登录页

### 商户端（`/merchant`）

- **商品管理** — 新增/编辑/删除商品（表格 + 弹窗 CRUD）
- **会话收件箱** — 会话列表（未读红点）+ 聊天窗口（实时 WS 通讯）
- 知识库 / AI设置 / 经营数据统计 — 占位菜单（后续迭代）

### 登录与身份隔离

系统支持用户端和商户端**同时保持登录**：

- 用户端登录态存在 `localStorage` 的 `user_*` 前缀下
- 商户端登录态存在 `localStorage` 的 `merchant_*` 前缀下
- `useAuthStore()` 根据当前路由路径自动返回对应身份的 Store 实例
- 登录页（`/login`）不属于任何分支，两个表单分别显式调用 `useUserAuthStore()` / `useMerchantAuthStore()`

## 开发

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:5173

### 构建生产版本

```bash
npm run build
```

### 预览生产构建

```bash
npm run preview
```

## API 地址

axios 实例直接请求 `http://localhost:8080`，后端需允许 `http://localhost:5173` 的 CORS 跨域。

## 接口一览

| 模块 | 接口 | 说明 |
|------|------|------|
| 会话 | `GET /session/user/list` | 用户查询自己的会话列表 |
| 会话 | `GET /session/ct/list` | 商户查询自己的会话列表 |
| 会话 | `GET /session/{sessionId}` | 查询会话详情 |
| 消息 | `GET /session/{sessionId}/message/list` | 查询消息列表 |
| 消息 | `PUT /session/{sessionId}/message/read` | 标记消息已读 |
| 消息 | `GET /session/{sessionId}/message/unread-count` | 未读消息数 |
| 商品 | `GET /goods/all` | 查询全部商品（公开） |
| 商品 | `GET /goods/mine` | 查询我的商品（需登录） |
| 商品 | `POST /goods/add` | 新增商品 |
| 商品 | `PUT /goods/update` | 更新商品 |
| 商品 | `DELETE /goods/delete` | 删除商品 |
| 商品 | `GET /goods/detail` | 商品详情 |
| 工作台 | `GET /workbench/menu` | 商户工作台菜单 |
| 用户 | `POST /user/login` | 用户登录 |
| 用户 | `POST /user/register` | 用户注册 |
| 用户 | `GET /user/{id}/name` | 按 ID 查用户名 |
| 商户 | `POST /commercialTenant/login` | 商户登录 |
| 商户 | `POST /commercialTenant/register` | 商户注册 |
| 商户 | `GET /commercialTenant/{id}/name` | 按 ID 查商户名 |

## WebSocket 通讯

| 端 | WS 地址 |
|------|---------|
| 用户端 | `ws://localhost:8080/user/chat/{userId}?token={token}` |
| 商户端 | `ws://localhost:8080/commercialTenant/chat/{ctId}?token={token}` |

详见 `src/ws/chat.js` 中的注释。
