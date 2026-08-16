<template>
  <div class="session-list">
    <el-table
        :data="sessions"
        v-loading="loading"
        @row-click="handleRowClick"
        class="session-table"
        row-class-name="session-row"
    >
      <el-table-column label="会话信息" min-width="200">
        <template #default="{ row }">
          <div class="session-info">
            <div class="session-partner">
              <el-tag v-if="type === 'USER'" size="small" type="warning">商户</el-tag>
              <el-tag v-else size="small" type="success">用户</el-tag>
              <span class="partner-name">{{ getPartnerName(row) }}</span>
            </div>
            <div class="session-goods">商品：{{ getGoodsName(row) }}</div>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="最后消息" min-width="200">
        <template #default="{ row }">
          <div class="last-message">
            <span class="message-preview">
              {{ truncate(row.lastMessageContent || '暂无消息', 50) }}
            </span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="时间" width="140">
        <template #default="{ row }">
          <RelativeTime :date="row.lastMessageTime" />
        </template>
      </el-table-column>

      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-badge
              v-if="row.unreadCount > 0"
              :value="row.unreadCount > 99 ? '99+' : row.unreadCount"
              class="unread-badge"
          />
          <span v-else class="no-unread" />
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
        v-if="total > 0"
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="pagination"
        @current-change="fetchData"
        @size-change="fetchData"
    />

    <el-empty v-if="!loading && sessions.length === 0" description="暂无会话" />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { sessionApi, goodsApi, userApi, commercialTenantApi } from '@/api'
import RelativeTime from './RelativeTime.vue'

const props = defineProps({
  type: { type: String, required: true }
})

const router = useRouter()
const sessions = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

// goodsId -> 商品名称 缓存。会话接口（/session/user/list、/session/ct/list）本身不
// 返回商品名称/商户名称/用户名称，只有 goodsId/ctId/userId，需要各自换名称接口查一次。
const goodsNameMap = ref({})
// ctId/userId -> 对方名称 缓存：用户端列表按 ctId 查商户名（commercialTenantApi.name），
// 商户端列表按 userId 查用户名（userApi.name），查不到就继续用编号兜底
const partnerNameMap = ref({})

async function resolveGoodsNames(rows) {
  const idsToFetch = [...new Set(
      rows.map((row) => row.goodsId).filter((id) => id != null && !(id in goodsNameMap.value))
  )]
  if (idsToFetch.length === 0) return

  const entries = await Promise.all(
      idsToFetch.map(async (id) => {
        try {
          const goods = await goodsApi.detail({ id })
          return [id, goods?.name || '商品']
        } catch {
          return [id, '商品']
        }
      })
  )
  // 整体替换触发响应式更新，避免 Map 直接赋值不触发表格重渲染
  goodsNameMap.value = { ...goodsNameMap.value, ...Object.fromEntries(entries) }
}

async function resolvePartnerNames(rows) {
  const idKey = props.type === 'USER' ? 'ctId' : 'userId'
  const nameApi = props.type === 'USER' ? commercialTenantApi.name : userApi.name
  const idsToFetch = [...new Set(
      rows.map((row) => row[idKey]).filter((id) => id != null && !(id in partnerNameMap.value))
  )]
  if (idsToFetch.length === 0) return

  const entries = await Promise.all(
      idsToFetch.map(async (id) => {
        try {
          const name = await nameApi(id)
          return [id, name || null]
        } catch {
          // 查不到就先不写入缓存，继续用编号兜底展示
          return [id, null]
        }
      })
  )
  partnerNameMap.value = {
    ...partnerNameMap.value,
    ...Object.fromEntries(entries.filter(([, name]) => name))
  }
}

async function fetchData() {
  loading.value = true
  try {
    const api = props.type === 'USER' ? sessionApi.listByUser : sessionApi.listByTenant
    const data = await api({ pageNum: pageNum.value, pageSize: pageSize.value })
    sessions.value = data.content || []
    total.value = data.totalElements || 0
    await Promise.all([resolveGoodsNames(sessions.value), resolvePartnerNames(sessions.value)])
  } finally {
    loading.value = false
  }
}

function handleRowClick(row) {
  const basePath = props.type === 'USER' ? '/user/inbox' : '/merchant/inbox'
  router.push(`${basePath}/${row.id}`)
}

function getPartnerName(row) {
  // 名字异步查询，回来之前（或查询失败）先用编号兜底，不阻塞列表渲染
  if (props.type === 'USER') {
    if (row.ctId == null) return '商户'
    return partnerNameMap.value[row.ctId] || `#${row.ctId}`
  }
  if (row.userId == null) return '用户'
  return partnerNameMap.value[row.userId] || `#${row.userId}`
}

function getGoodsName(row) {
  if (row.goodsId == null) return '商品'
  return goodsNameMap.value[row.goodsId] || '商品'
}

function truncate(text, maxLen) {
  if (!text) return ''
  return text.length > maxLen ? text.slice(0, maxLen) + '...' : text
}

function handleSessionRead(event) {
  const sessionId = Number(event?.detail?.sessionId)
  if (!sessionId) return

  const hasUnread = sessions.value.some((row) => Number(row.id) === sessionId && Number(row.unreadCount || 0) > 0)
  if (hasUnread) {
    fetchData()
  }
}

onMounted(() => {
  fetchData()
  window.addEventListener('chat:session-read', handleSessionRead)
})

onUnmounted(() => {
  window.removeEventListener('chat:session-read', handleSessionRead)
})
</script>

<style scoped>
.session-list {
  flex: 1;
  min-height: 0;
  padding: 20px;
}

.session-table {
  cursor: pointer;
}

.session-row:hover td {
  background-color: #f5f7fa;
}

.session-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.session-partner {
  display: flex;
  align-items: center;
  gap: 8px;
}

.partner-name {
  font-weight: 500;
  color: #333;
}

.session-goods {
  font-size: 12px;
  color: #999;
  margin-left: 48px;
}

.last-message {
  font-size: 13px;
  color: #666;
}

.message-preview {
  word-break: break-all;
}

.no-unread {
  display: inline-block;
  width: 8px;
  height: 8px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>