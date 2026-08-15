<template>
  <el-container class="layout-container">
    <el-aside width="200px">
      <div class="logo">AI 客服系统</div>
      <el-menu
        :default-active="activeMenu"
        class="side-menu"
        @select="handleMenuSelect"
      >
        <el-menu-item index="/user/goods">
          <span>商品</span>
        </el-menu-item>
        <el-menu-item index="/user/consult">
          <span>咨询</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
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
      </el-header>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const activeMenu = ref(route.path || '/user/goods')

function handleMenuSelect(index) {
  if (index === '/user/goods') {
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
