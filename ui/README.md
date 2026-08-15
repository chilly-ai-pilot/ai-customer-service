# AI 客服系统 - 前端

基于 Vue 3 的 AI 客服系统前端项目，使用 Vite 构建。

## 技术栈

- **Vue 3** - 渐进式 JavaScript 框架
- **Vue Router 4** - 官方路由管理
- **Pinia** - 状态管理
- **Element Plus** - UI 组件库
- **Axios** - HTTP 请求库
- **Vite** - 下一代前端构建工具

## 项目结构

```
ui/
├── src/
│   ├── api/           # API 接口封装
│   ├── assets/        # 静态资源
│   ├── components/    # 公共组件
│   ├── router/        # 路由配置
│   ├── stores/        # Pinia 状态管理
│   ├── views/         # 页面视图
│   ├── App.vue        # 根组件
│   └── main.js        # 入口文件
├── index.html
├── vite.config.js
└── package.json
```

## 功能模块

### 用户端 (`/user`)
- 商品浏览
- 客服咨询

### 商户端 (`/merchant`)
- 商品管理（增删改查）
- 知识库管理
- AI 配置
- 会话收件箱
- 经营数据统计

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

## API 代理

开发环境下，Vite 会将 `/api` 开头的请求代理到 `http://localhost:8080`（后端服务）。

## 登录说明

系统支持两种登录入口：
- **用户登录** - 普通用户入口
- **商户登录** - 商户/管理员入口

登录后根据角色自动跳转到对应工作台。
