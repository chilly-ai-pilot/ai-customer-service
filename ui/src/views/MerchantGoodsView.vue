<template>
  <div class="goods-manager">
    <div class="toolbar">
      <h2>商品管理</h2>
      <el-button type="primary" @click="openAddDialog">新增商品</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="100" />
      <el-table-column prop="name" label="商品名称" />
      <el-table-column prop="ctId" label="所属商户ID" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-button type="primary" text @click="openEditDialog(row)">编辑</el-button>
            <el-button type="danger" text @click="handleDelete(row.id)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      class="pagination"
      @current-change="fetchData"
      @size-change="fetchData"
    />

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="400px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入商品名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { goodsApi } from '@/api'

const tableData = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const dialogTitle = ref('新增商品')
const formRef = ref()
const form = ref({ id: null, name: '' })
const isEdit = ref(false)

const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }]
}

async function fetchData() {
  loading.value = true
  try {
    const data = await goodsApi.mine({ pageNum: pageNum.value, pageSize: pageSize.value })
    tableData.value = data.content
    total.value = data.totalElements
  } finally {
    loading.value = false
  }
}

function openAddDialog() {
  isEdit.value = false
  dialogTitle.value = '新增商品'
  form.value = { id: null, name: '' }
  formRef.value?.resetFields()
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  dialogTitle.value = '编辑商品'
  form.value = { id: row.id, name: row.name }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (isEdit.value) {
      await goodsApi.update({ id: form.value.id, name: form.value.name })
      ElMessage.success('更新成功')
    } else {
      await goodsApi.add({ name: form.value.name })
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) {
    // error handled by interceptor
  }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除该商品吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await goodsApi.delete({ id })
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {
    // user cancelled or error
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.goods-manager {
  background-color: #fff;
  padding: 20px;
  border-radius: 4px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.toolbar h2 {
  margin: 0;
  font-size: 18px;
}

.action-buttons {
  display: flex;
  gap: 4px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
