<template>
  <div class="oa-notice-list">
    <el-card shadow="hover">
      <template #header>
        <span class="card-title">公告通知</span>
      </template>

      <el-table
        :data="noticeList"
        stripe
        style="width: 100%"
        v-loading="loading"
        @row-click="handleRowClick"
        row-class-name="clickable-row"
      >
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-content" v-loading="detailLoading[row.id]">
              <template v-if="detailMap[row.id]">
                <p class="detail-text">{{ detailMap[row.id].content }}</p>
                <p class="detail-time" v-if="detailMap[row.id].publishTime">
                  发布时间：{{ detailMap[row.id].publishTime }}
                </p>
              </template>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="公告标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="content" label="公告内容" min-width="300" show-overflow-tooltip>
          <template #default="{ row }">
            {{ truncateContent(row.content) }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="180" align="center" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getNoticePage, getNoticeDetail, markNoticeRead } from '@/api/oa/notice'

defineOptions({ name: 'OaNoticeList' })

interface NoticeRecord {
  id: number
  title: string
  content: string
  createTime: string
  publishTime?: string
}

interface NoticeDetail {
  id: number
  title: string
  content: string
  createTime: string
  publishTime: string
}

const loading = ref(false)
const total = ref(0)
const noticeList = ref<NoticeRecord[]>([])
const detailMap = reactive<Record<number, NoticeDetail>>({})
const detailLoading = reactive<Record<number, boolean>>({})
const expandedRows = reactive<Set<number>>(new Set())

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10
})

/** 截断内容显示 */
const truncateContent = (content: string): string => {
  if (!content) return ''
  return content.length > 50 ? content.substring(0, 50) + '...' : content
}

/** 点击行展开查看详情 */
const handleRowClick = async (row: NoticeRecord) => {
  if (expandedRows.has(row.id)) {
    expandedRows.delete(row.id)
    return
  }
  expandedRows.add(row.id)

  // 已加载过详情则不再请求
  if (detailMap[row.id]) return

  detailLoading[row.id] = true
  try {
    const res = await getNoticeDetail(row.id)
    const detail = res.data ?? res
    detailMap[row.id] = detail
    // 标记为已读
    await markNoticeRead(row.id).catch(() => {})
  } catch {
    ElMessage.error('获取公告详情失败')
  } finally {
    detailLoading[row.id] = false
  }
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
.oa-notice-list {
  padding: 20px;
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
.expand-content {
  padding: 16px 24px;
}
.detail-text {
  line-height: 1.8;
  white-space: pre-wrap;
  color: #333;
}
.detail-time {
  margin-top: 12px;
  color: #999;
  font-size: 13px;
}
.clickable-row {
  cursor: pointer;
}
</style>
