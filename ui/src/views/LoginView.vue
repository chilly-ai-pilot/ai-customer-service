<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="card-header">
          <span>AI 客服系统</span>
        </div>
      </template>
      <el-tabs v-model="activeTab" class="login-tabs">
        <el-tab-pane label="用户登录" name="user">
          <el-form ref="userFormRef" :model="userForm" :rules="rules" label-width="0" class="login-form">
            <el-form-item prop="account">
              <el-input v-model="userForm.account" placeholder="账号" prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="userForm.password" type="password" placeholder="密码" prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" class="submit-btn" :loading="userLoading" @click="handleUserLogin">
                登录
              </el-button>
            </el-form-item>
            <div class="form-footer">
              没有账号？
              <el-link type="primary" @click="goToRegister('user')">立即注册</el-link>
            </div>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="商户登录" name="merchant">
          <el-form ref="merchantFormRef" :model="merchantForm" :rules="rules" label-width="0" class="login-form">
            <el-form-item prop="account">
              <el-input v-model="merchantForm.account" placeholder="账号" prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="merchantForm.password" type="password" placeholder="密码" prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" class="submit-btn" :loading="merchantLoading" @click="handleMerchantLogin">
                登录
              </el-button>
            </el-form-item>
            <div class="form-footer">
              没有账号？
              <el-link type="primary" @click="goToRegister('merchant')">立即注册</el-link>
            </div>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="registerDialogVisible" :title="registerTitle" width="400px">
      <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-width="60px">
        <el-form-item label="账号" prop="account">
          <el-input v-model="registerForm.account" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="registerForm.name" placeholder="请输入名称" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="registerDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="registerLoading" @click="handleRegister">注册</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { commercialTenantApi, userApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const activeTab = ref('user')
const userFormRef = ref()
const merchantFormRef = ref()
const registerFormRef = ref()

const userForm = ref({ account: '', password: '' })
const merchantForm = ref({ account: '', password: '' })
const registerForm = ref({ account: '', name: '', password: '' })
const registerType = ref('')
const registerDialogVisible = ref(false)
const userLoading = ref(false)
const merchantLoading = ref(false)
const registerLoading = ref(false)

const rules = {
  account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerRules = {
  account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerTitle = computed(() => registerType.value === 'user' ? '用户注册' : '商户注册')

async function handleUserLogin() {
  const valid = await userFormRef.value.validate().catch(() => false)
  if (!valid) return
  userLoading.value = true
  try {
    const data = await userApi.login(userForm.value)
    authStore.setAuth(data.token, data, 'USER')
    ElMessage.success('登录成功')
    router.push('/user/goods')
  } finally {
    userLoading.value = false
  }
}

async function handleMerchantLogin() {
  const valid = await merchantFormRef.value.validate().catch(() => false)
  if (!valid) return
  merchantLoading.value = true
  try {
    const data = await commercialTenantApi.login(merchantForm.value)
    authStore.setAuth(data.token, data, 'TENANT')
    ElMessage.success('登录成功')
    router.push('/merchant/goods')
  } finally {
    merchantLoading.value = false
  }
}

function goToRegister(type) {
  registerType.value = type
  registerFormRef.value?.resetFields()
  registerDialogVisible.value = true
}

async function handleRegister() {
  const valid = await registerFormRef.value.validate().catch(() => false)
  if (!valid) return
  registerLoading.value = true
  try {
    if (registerType.value === 'user') {
      await userApi.register(registerForm.value)
    } else {
      await commercialTenantApi.register(registerForm.value)
    }
    ElMessage.success('注册成功，请登录')
    registerDialogVisible.value = false
    activeTab.value = registerType.value === 'user' ? 'user' : 'merchant'
  } finally {
    registerLoading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
}

.card-header {
  text-align: center;
  font-size: 20px;
  font-weight: bold;
}

.login-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

.login-form {
  margin-top: 10px;
}

.submit-btn {
  width: 100%;
}

.form-footer {
  text-align: center;
  color: #666;
}
</style>
