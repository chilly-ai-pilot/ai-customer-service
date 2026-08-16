<template>
  <div class="layout-container">
    <aside class="aside">
      <div class="logo">AI 客服系统</div>
      <el-menu
        :default-active="resolvedActiveMenu"
        class="side-menu"
        @select="handleMenuSelect"
      >
        <el-menu-item v-for="item in menuList" :key="item.name" :index="item.name">
          <span>{{ item.name }}</span>
          <!-- 会话收件箱菜单显示未读数红点 -->
          <el-badge
            v-if="item.name === '会话收件箱' && totalUnreadCount > 0"
            :value="totalUnreadCount > 99 ? '99+' : totalUnreadCount"
            class="inbox-badge"
          />
        </el-menu-item>
      </el-menu>
    </aside>
    <div class="right">
      <header class="header">
        <div class="header-title">商户工作台</div>
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
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { workbenchApi, sessionApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

// 工作台菜单列表
const menuList = ref([])
// 未读消息总数（用于红点显示）
const totalUnreadCount = ref(0)

// 菜单名称 -> 前端路由 映射（后端只返回 name/path，前端自己维护路由映射）
const ROUTE_BY_MENU_NAME = {
  '商品管理': '/merchant/goods',
  '会话收件箱': '/merchant/inbox'
}

// 根据当前路由计算激活的菜单项
const resolvedActiveMenu = computed(() => {
  if (route.path.startsWith('/merchant/inbox')) return '会话收件箱'
  if (route.path.startsWith('/merchant/goods')) return '商品管理'
  return ''
})

/**
 * 获取未读消息总数。
 * 会话列表接口没有单独的"总未读数"字段，只能先拉全部会话再求和。
 * 先用 pageSize=1 探出 totalElements，再一次性把全部会话拉回来。
 */
async function fetchUnreadCount() {
  try {
    const peek = await sessionApi.listByTenant({ pageNum: 1, pageSize: 1 })
    const total = peek.totalElements || 0
    if (total === 0) {
      totalUnreadCount.value = 0
      return
    }
    const data = await sessionApi.listByTenant({ pageNum: 1, pageSize: total })
    totalUnreadCount.value = (data.content || []).reduce((sum, s) => sum + (s.unreadCount || 0), 0)
  } catch {
    totalUnreadCount.value = 0
  }
}

/** 监听全局"会话已读"事件（由 ChatWindow 触发），刷新未读数 */
function handleSessionRead() {
  fetchUnreadCount()
}

onMounted(async () => {
  try {
    // 获取工作台菜单
    menuList.value = await workbenchApi.menu()
    await fetchUnreadCount()
  } catch (e) {
    menuList.value = []
  }
  window.addEventListener('chat:session-read', handleSessionRead)
})

onUnmounted(() => {
  window.removeEventListener('chat:session-read', handleSessionRead)
})

// 路由变化时，若进入收件箱则刷新未读数
watch(() => route.path, (newPath) => {
  if (newPath.startsWith('/merchant/inbox')) {
    fetchUnreadCount()
  }
})

/**
 * 处理侧边栏菜单点击。
 * placeholder=true 的占位模块提示"功能开发中"，其余按路由映射跳转。
 */
function handleMenuSelect(menuName) {
  const item = menuList.value.find((m) => m.name === menuName)
  if (!item || item.placeholder) {
    ElMessage.info('功能开发中')
    return
  }
  const target = ROUTE_BY_MENU_NAME[item.name]
  if (!target) {
    ElMessage.info('功能开发中')
    return
  }
  router.push(target)
  if (target === '/merchant/inbox') {
    fetchUnreadCount()
  }
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

.right {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
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

.inbox-badge {
  margin-left: 6px;
  line-height: 1;
}
</style>
