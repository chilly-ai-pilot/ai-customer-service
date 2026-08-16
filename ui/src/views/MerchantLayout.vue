<template>
  <div class="layout-container">
    <aside class="aside">
      <div class="logo">AI 客服系统</div>
      <el-menu
        :default-active="resolvedActiveMenu"
        class="side-menu"
        @select="handleMenuSelect"
      >
        <el-menu-item v-for="item in menuList" :key="item.path" :index="item.path">
          <span>{{ item.name }}</span>
          <el-badge
            v-if="item.path === '/merchant/inbox' && totalUnreadCount > 0"
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

const menuList = ref([])
const activeMenu = ref('/merchant/goods')

const resolvedActiveMenu = computed(() => {
  if (activeMenu.value.startsWith('/merchant/inbox')) return '/merchant/inbox'
  return activeMenu.value
})
const totalUnreadCount = ref(0)

async function fetchUnreadCount() {
  try {
    const data = await sessionApi.listByTenant({ pageNum: 1, pageSize: 100 })
    totalUnreadCount.value = data.content.reduce((sum, s) => sum + (s.unreadCount || 0), 0)
  } catch {
    totalUnreadCount.value = 0
  }
}

function handleSessionRead() {
  fetchUnreadCount()
}

onMounted(async () => {
  try {
    const data = await workbenchApi.menu()
    menuList.value = data.map(item => ({
      ...item,
      path: '/merchant' + item.path
    }))
    const firstMenu = data.find(item => !item.placeholder)
    if (firstMenu) {
      activeMenu.value = '/merchant' + firstMenu.path
    }
    await fetchUnreadCount()
  } catch (e) {
    menuList.value = []
  }

  window.addEventListener('chat:session-read', handleSessionRead)
})

onUnmounted(() => {
  window.removeEventListener('chat:session-read', handleSessionRead)
})

watch(() => route.path, (newPath) => {
  activeMenu.value = newPath
  if (newPath.startsWith('/merchant/inbox')) {
    fetchUnreadCount()
  }
})

function handleMenuSelect(index) {
  if (index === '/merchant/goods' || index === '/merchant/inbox') {
    router.push(index)
    if (index === '/merchant/inbox') {
      fetchUnreadCount()
    }
  } else {
    ElMessage.info('功能开发中')
  }
}

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
