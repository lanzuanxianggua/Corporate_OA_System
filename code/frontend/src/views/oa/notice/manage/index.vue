<template>
  <div class="oa-notice-manage">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span class="card-title">公告管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增公告
          </el-button>
        </div>
      </template>

      <el-table :data="noticeList" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="title" label="公告标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="content" label="公告内容" min-width="300" show-overflow-tooltip>
          <template #default="{ row }">
            {{ truncateContent(row.content) }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : row.status === 0 ? 'info' : 'warning'">
              {{ row.status === 1 ? '已发布' : row.status === 0 ? '草稿' : '已撤回' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status !== 1"
              type="success"
              link
              size="small"
              @click="handlePublish(row)"
            >
              发布
            </el-button>
            <el-button
              v-if="row.status === 1"
              type="warning"
              link
              size="small"
              @click="handleWithdraw(row)"
            >
              撤回
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="fetchNoticeList"
          @current-change="fetchNoticeList"
        />
      </div>
    </el-card>

    <!-- 新增公告弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="新增公告"
      width="640px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="80px"
      >
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="公告内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="8"
            placeholder="请输入公告内容"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getNoticePage, addNotice, publishNotice, withdrawNotice } from '@/api/oa/notice'

defineOptions({ name: 'OaNoticeManage' })

interface NoticeRecord {
  id: number
  title: string
  content: string
  createTime: string
  status: number
}

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const total = ref(0)
const noticeList = ref<NoticeRecord[]>([])
const formRef = ref<FormInstance>()

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10
})

const form = reactive({
  title: '',
  content: ''
})

const rules = reactive<FormRules>({
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
})

/** 截断内容显示 */
const truncateContent = (content: string): string => {
  if (!content) return ''
  return content.length > 50 ? content.substring(0, 50) + '...' : content
}

/** 打开新增弹窗 */
const handleAdd = () => {
  dialogVisible.value = true
}

/** 提交新增公告 */
const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await addNotice({ title: form.title, content: form.content })
    ElMessage.success('公告创建成功')
    dialogVisible.value = false
    fetchNoticeList()
  } catch {
    ElMessage.error('创建失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

/** 发布公告 */
const handlePublish = async (row: NoticeRecord) => {
  await ElMessageBox.confirm(`确认发布公告「${row.title}」？`, '发布确认', {
    confirmButtonText: '发布',
    cancelButtonText: '取消',
    type: 'info'
  })
  try {
    await publishNotice(row.id)
    ElMessage.success('发布成功')
    fetchNoticeList()
  } catch {
    ElMessage.error('发布失败')
  }
}

/** 撤回公告 */
const handleWithdraw = async (row: NoticeRecord) => {
  await ElMessageBox.confirm(`确认撤回公告「${row.title}」？`, '撤回确认', {
    confirmButtonText: '撤回',
    cancelButtonText: '取消',
    type: 'warning'
  })
  try {
    await withdrawNotice(row.id)
    ElMessage.success('已撤回')
    fetchNoticeList()
  } catch {
    ElMessage.error('撤回失败')
  }
}

/** 重置表单 */
const resetForm = () => {
  formRef.value?.resetFields()
}

/** 获取公告分页列表 */
const fetchNoticeList = async () => {
  loading.value = true
  try {
    const res = await getNoticePage(queryParams)
    const data = res.data ?? res
    noticeList.value = data.records ?? data.list ?? []
    total.value = data.total ?? 0
  } catch {
    ElMessage.error('获取公告列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchNoticeList()
})
</script>

<style scoped>
.oa-notice-manage {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
}
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
