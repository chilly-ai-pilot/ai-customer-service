<template>
  <el-container class="layout-container">
    <el-aside width="200px">
      <div class="logo">AI 客服系统</div>
      <el-menu
        :default-active="activeMenu"
        class="side-menu"
        @select="handleMenuSelect"
      >
        <el-menu-item v-for="item in menuList" :key="item.path" :index="item.path">
          <span>{{ item.name }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
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
      </el-header>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { workbenchApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const menuList = ref([])
const activeMenu = ref('/merchant/goods')

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
  } catch (e) {
    menuList.value = []
  }
})

function handleMenuSelect(index) {
  if (index === '/merchant/goods') {
    router.push(index)
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
}

.el-aside {
  background-color: #304156;
  color: #fff;
  padding: 0;
}

.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  font-size: 18px;
  font-weight: bold;
  color: #fff;
  background-color: #1f2d3d;
}

.side-menu {
  border-right: none;
  background-color: #304156;
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
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
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
  background-color: #f0f2f5;
  padding: 0;
}
</style>
