<template>
  <div class="oa-leave-approval">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span class="card-title">请假审批</span>
          <el-button type="primary" link @click="fetchPendingList">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>

      <el-table :data="pendingList" stripe style="width: 100%" v-loading="loading" empty-text="暂无待审批记录">
        <el-table-column prop="empName" label="申请人" width="100" align="center" />
        <el-table-column prop="leaveType" label="请假类型" width="100" align="center">
          <template #default="{ row }">
            {{ leaveTypeMap[row.leaveType] }}
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="180" align="center" />
        <el-table-column prop="endTime" label="结束时间" width="180" align="center" />
        <el-table-column prop="days" label="天数" width="80" align="center" />
        <el-table-column prop="reason" label="请假原因" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType[row.status]">
              {{ statusText[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              type="success"
              link
              size="small"
              @click="handleApprove(row)"
            >
              通过
            </el-button>
            <el-button
              type="danger"
              link
              size="small"
              @click="handleReject(row)"
            >
              驳回
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getPendingLeaves, approveLeave } from '@/api/oa/leave'

defineOptions({ name: 'OaLeaveApproval' })

interface PendingLeave {
  id: number
  empName: string
  leaveType: number
  startTime: string
  endTime: string
  days: number
  reason: string
  status: number
}

const leaveTypeMap: Record<number, string> = {
  1: '事假',
  2: '病假',
  3: '年假',
  4: '调休'
}

const statusText: Record<number, string> = {
  1: '审批中',
  2: '已通过',
  3: '已驳回',
  4: '已撤回'
}

const statusTagType: Record<number, string> = {
  1: 'warning',
  2: 'success',
  3: 'danger',
  4: 'info'
}

const loading = ref(false)
const pendingList = ref<PendingLeave[]>([])

/** 审批通过 */
const handleApprove = async (row: PendingLeave) => {
  await ElMessageBox.confirm(`确认通过 ${row.empName} 的请假申请？`, '审批确认', {
    confirmButtonText: '通过',
    cancelButtonText: '取消',
    type: 'info'
  })
  try {
    await approveLeave(row.id, { status: 2 })
    ElMessage.success('已通过')
    fetchPendingList()
  } catch {
    ElMessage.error('操作失败')
  }
}

/** 审批驳回 */
const handleReject = async (row: PendingLeave) => {
  await ElMessageBox.confirm(`确认驳回 ${row.empName} 的请假申请？`, '审批确认', {
    confirmButtonText: '驳回',
    cancelButtonText: '取消',
    type: 'warning'
  })
  try {
    await approveLeave(row.id, { status: 3 })
    ElMessage.success('已驳回')
    fetchPendingList()
  } catch {
    ElMessage.error('操作失败')
  }
}

/** 获取待审批列表 */
const fetchPendingList = async () => {
  loading.value = true
  try {
    const res = await getPendingLeaves()
    pendingList.value = res.data ?? res
  } catch {
    ElMessage.error('获取待审批列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchPendingList()
})
</script>

<style scoped>
.oa-leave-approval {
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
</style>
