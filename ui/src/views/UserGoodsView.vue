<template>
  <div class="user-goods">
    <el-row :gutter="20">
      <el-col
        v-for="item in goodsList"
        :key="item.id"
        :xs="24"
        :sm="12"
        :md="8"
        :lg="6"
        class="goods-col"
      >
        <el-card class="goods-card" shadow="hover">
          <div class="goods-name">{{ item.name }}</div>
          <div class="card-footer">
            <el-button type="primary" plain round @click="handleConsult(item)">
              咨询
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && goodsList.length === 0" description="暂无商品" />

    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[8, 16, 24]"
      layout="total, sizes, prev, pager, next"
      class="pagination"
      @current-change="fetchData"
      @size-change="fetchData"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { goodsApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const goodsList = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)

async function fetchData() {
  loading.value = true
  try {
    const data = await goodsApi.all({ pageNum: pageNum.value, pageSize: pageSize.value })
    goodsList.value = data.content
    total.value = data.totalElements
  } finally {
    loading.value = false
  }
}

function handleConsult(item) {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  ElMessage.info('咨询功能开发中')
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.user-goods {
  padding: 10px;
}

.goods-col {
  margin-bottom: 20px;
}

.goods-card {
  border-radius: 12px;
  text-align: center;
  transition: transform 0.3s, box-shadow 0.3s;
}

.goods-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}

.goods-name {
  font-size: 18px;
  font-weight: bold;
  color: #333;
  padding: 30px 10px;
  min-height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-footer {
  padding: 15px 0 5px;
  border-top: 1px solid #f0f0f0;
}

.card-footer .el-button {
  width: 100px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
