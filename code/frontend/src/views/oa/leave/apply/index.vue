<template>
  <div class="oa-leave-apply">
    <el-card class="apply-card" shadow="hover">
      <template #header>
        <span class="card-title">请假申请</span>
      </template>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        style="max-width: 600px"
      >
        <el-form-item label="请假类型" prop="leaveType">
          <el-select v-model="form.leaveType" placeholder="请选择请假类型" style="width: 100%">
            <el-option label="事假" :value="1" />
            <el-option label="病假" :value="2" />
            <el-option label="年假" :value="3" />
            <el-option label="调休" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="请假时间" prop="dateRange">
          <el-date-picker
            v-model="form.dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 100%"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="请假原因" prop="reason">
          <el-input
            v-model="form.reason"
            type="textarea"
            :rows="4"
            placeholder="请输入请假原因"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            提交申请
          </el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="history-card" shadow="hover" style="margin-top: 20px">
      <template #header>
        <span class="card-title">我的请假记录</span>
      </template>
      <el-table :data="leaveList" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="leaveType" label="请假类型" width="100" align="center">
          <template #default="{ row }">
            {{ leaveTypeMap[row.leaveType] }}
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="180" align="center" />
        <el-table-column prop="endTime" label="结束时间" width="180" align="center" />
        <el-table-column prop="days" label="天数" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType[row.status]">
              {{ statusText[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="请假原因" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 1"
              type="warning"
              link
              size="small"
              @click="handleRevoke(row)"
            >
              撤回
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { submitLeave, getMyLeaves, revokeLeave } from '@/api/oa/leave'

defineOptions({ name: 'OaLeaveApply' })

interface LeaveRecord {
  id: number
  leaveType: number
  startTime: string
  endTime: string
  days: number
  status: number
  reason: string
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

const formRef = ref<FormInstance>()
const submitting = ref(false)
const loading = ref(false)
const leaveList = ref<LeaveRecord[]>([])

const form = reactive({
  leaveType: undefined as number | undefined,
  dateRange: [] as string[],
  reason: ''
})

const rules = reactive<FormRules>({
  leaveType: [{ required: true, message: '请选择请假类型', trigger: 'change' }],
  dateRange: [{ required: true, message: '请选择请假时间', trigger: 'change' }],
  reason: [{ required: true, message: '请输入请假原因', trigger: 'blur' }]
})

/** 提交请假申请 */
const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await submitLeave({
      leaveType: form.leaveType,
      startTime: form.dateRange[0],
      endTime: form.dateRange[1],
      reason: form.reason
    })
    ElMessage.success('请假申请已提交')
    resetForm()
    fetchLeaveList()
  } catch {
    ElMessage.error('提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

/** 重置表单 */
const resetForm = () => {
  formRef.value?.resetFields()
}

/** 撤回请假 */
const handleRevoke = async (row: LeaveRecord) => {
  await ElMessageBox.confirm('确认撤回该请假申请？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  try {
    await revokeLeave(row.id)
    ElMessage.success('已撤回')
    fetchLeaveList()
  } catch {
    ElMessage.error('撤回失败')
  }
}

/** 获取个人请假记录 */
const fetchLeaveList = async () => {
  loading.value = true
  try {
    const res = await getMyLeaves()
    leaveList.value = res.data ?? res
  } catch {
    ElMessage.error('获取请假记录失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchLeaveList()
})
</script>

<style scoped>
.oa-leave-apply {
  padding: 20px;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
}
</style>
