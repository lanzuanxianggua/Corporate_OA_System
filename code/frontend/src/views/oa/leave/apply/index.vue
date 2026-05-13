<template>
  <div class="leave-apply-container">
    <el-row :gutter="20">
      <el-col :span="14">
        <el-card>
          <template #header>
            <span>我的请假记录</span>
          </template>
          <el-table :data="leaveRecords" stripe v-loading="loading">
            <el-table-column prop="type" label="类型" width="100" />
            <el-table-column prop="startTime" label="开始时间" width="160" />
            <el-table-column prop="endTime" label="结束时间" width="160" />
            <el-table-column prop="days" label="天数" width="80" />
            <el-table-column prop="reason" label="原因" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :total="total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              @size-change="loadData"
              @current-change="loadData"
            />
          </div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card>
          <template #header>
            <span>提交请假申请</span>
          </template>
          <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
            <el-form-item label="请假类型" prop="type">
              <el-select v-model="form.type" placeholder="请选择请假类型" style="width: 100%">
                <el-option label="事假" value="事假" />
                <el-option label="病假" value="病假" />
                <el-option label="年假" value="年假" />
                <el-option label="婚假" value="婚假" />
                <el-option label="丧假" value="丧假" />
                <el-option label="产假" value="产假" />
              </el-select>
            </el-form-item>
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker
                v-model="form.startTime"
                type="datetime"
                placeholder="选择开始时间"
                style="width: 100%"
                value-format="YYYY-MM-DD HH:mm:ss"
              />
            </el-form-item>
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker
                v-model="form.endTime"
                type="datetime"
                placeholder="选择结束时间"
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
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSubmit" :loading="submitting">提交</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import { getLeavePage, submitLeave } from "@/api/leave";
import dayjs from "dayjs";

const formRef = ref<FormInstance>();
const submitting = ref(false);
const loading = ref(false);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

const leaveRecords = ref<any[]>([]);

const form = reactive({
  type: "",
  startTime: "",
  endTime: "",
  reason: ""
});

const rules: FormRules = {
  type: [{ required: true, message: "请选择请假类型", trigger: "change" }],
  startTime: [{ required: true, message: "请选择开始时间", trigger: "change" }],
  endTime: [{ required: true, message: "请选择结束时间", trigger: "change" }],
  reason: [{ required: true, message: "请输入请假原因", trigger: "blur" }]
};

const getStatusType = (status: number) => {
  const map: Record<number, string> = {
    0: "warning",
    1: "success",
    2: "danger"
  };
  return map[status] || "info";
};

const getStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: "待审批",
    1: "已通过",
    2: "已拒绝"
  };
  return map[status] || "未知";
};

const loadData = async () => {
  try {
    loading.value = true;
    const res: any = await getLeavePage({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      status: undefined
    });
    if (res.data?.list) {
      leaveRecords.value = res.data.list;
      total.value = res.data.total;
    }
  } catch (error) {
    console.error("获取请假记录失败", error);
  } finally {
    loading.value = false;
  }
};

const handleSubmit = async () => {
  if (!formRef.value) return;

  await formRef.value.validate(async (valid) => {
    if (!valid) return;

    try {
      submitting.value = true;
      const start = dayjs(form.startTime);
      const end = dayjs(form.endTime);
      const days = end.diff(start, "day") + 1;

      await submitLeave({
        type: form.type,
        startTime: form.startTime,
        endTime: form.endTime,
        days,
        reason: form.reason
      });
      ElMessage.success("提交成功");
      handleReset();
      loadData();
    } catch (error: any) {
      ElMessage.error(error.message || "提交失败");
    } finally {
      submitting.value = false;
    }
  });
};

const handleReset = () => {
  formRef.value?.resetFields();
};

onMounted(() => {
  loadData();
});
</script>

<style scoped lang="scss">
.leave-apply-container {
  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>