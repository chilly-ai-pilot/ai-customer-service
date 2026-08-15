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