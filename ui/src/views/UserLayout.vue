<template>
  <div class="layout-container">
    <aside class="aside">
      <div class="logo">AI 客服系统</div>
      <el-menu
        :default-active="activeMenu"
        class="side-menu"
        @select="handleMenuSelect"
      >
        <el-menu-item index="/user/goods">
          <span>商品</span>
        </el-menu-item>
        <el-menu-item index="/user/inbox">
          <span>我的咨询</span>
        </el-menu-item>
      </el-menu>
    </aside>
    <div class="right">
      <header class="header">
        <div class="header-title">用户工作台</div>
        <el-dropdown @command="handleCommand">
          <span class="user-info">
            {{ authStore.userInfo?.name }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>
      <main class="main-content">
        <router-view :key="$route.fullPath" />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

/** 记录上一次访问的收件箱路由（含 sessionId），切换菜单时跳回该路由 */
const lastInboxRoute = ref('/user/inbox')

// 根据当前路由路径计算激活菜单项
const activeMenu = computed(() => {
  if (route.path.startsWith('/user/inbox')) return '/user/inbox'
  return route.path
})

// 监听路由变化，记录最近一次访问的收件箱路由
watch(
  () => route.fullPath,
  (fullPath) => {
    if (fullPath.startsWith('/user/inbox')) {
      lastInboxRoute.value = fullPath
    }
  },
  { immediate: true }
)

/**
 * 处理侧边栏菜单点击。
 * 收件箱菜单：跳回最近一次访问的收件箱路由（含 sessionId 的详情页或空列表页）。
 */
function handleMenuSelect(index) {
  if (index === '/user/inbox') {
    router.push(lastInboxRoute.value || '/user/inbox')
    return
  }
  router.push(index)
}

/** 处理右上角下拉菜单命令 */
function handleCommand(command) {
  if (command === 'logout') {
    authStore.clearAuth()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  display: flex;
  overflow: hidden;
}

.aside {
  width: 200px;
  flex-shrink: 0;
  background-color: #304156;
  color: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.logo {
  height: 60px;
  flex-shrink: 0;
  line-height: 60px;
  text-align: center;
  font-size: 18px;
  font-weight: bold;
  color: #fff;
  background-color: #1f2d3d;
}

.side-menu {
  flex: 1;
  border-right: none;
  background-color: #304156;
  overflow-y: auto;
}

.side-menu :deep(.el-menu-item) {
  color: #bfcbd9;
}

.side-menu :deep(.el-menu-item:hover),
.side-menu :deep(.el-menu-item.is-active) {
  background-color: #263445;
  color: #409eff;
}

.right {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header {
  height: 60px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  padding: 0 20px;
}

.header-title {
  font-size: 18px;
  font-weight: bold;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: #333;
}

.main-content {
  flex: 1;
  min-height: 0;
  background-color: #f0f2f5;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.main-content > * {
  flex: 1;
  min-height: 0;
}
</style>
