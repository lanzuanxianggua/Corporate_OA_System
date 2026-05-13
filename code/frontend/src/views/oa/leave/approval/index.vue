<template>
  <div class="leave-approval-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>请假审批</span>
          <el-radio-group v-model="status">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="待审批">待审批</el-radio-button>
            <el-radio-button label="已通过">已通过</el-radio-button>
            <el-radio-button label="已拒绝">已拒绝</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <el-table :data="tableData" stripe>
        <el-table-column prop="applicant" label="申请人" width="100" />
        <el-table-column prop="dept" label="部门" width="100" />
        <el-table-column prop="type" label="类型" width="80" />
        <el-table-column prop="timeRange" label="时间范围" width="220" />
        <el-table-column prop="days" label="天数" width="80" />
        <el-table-column prop="reason" label="原因" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === '待审批'">
              <el-button type="success" size="small" @click="handleApprove(row)">通过</el-button>
              <el-button type="danger" size="small" @click="handleReject(row)">拒绝</el-button>
            </template>
            <span v-else>-</span>
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
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" title="审批请假申请" width="500px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="申请人">{{ currentRow?.applicant }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ currentRow?.dept }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ currentRow?.type }}</el-descriptions-item>
        <el-descriptions-item label="时间范围">{{ currentRow?.timeRange }}</el-descriptions-item>
        <el-descriptions-item label="天数">{{ currentRow?.days }}</el-descriptions-item>
        <el-descriptions-item label="原因">{{ currentRow?.reason }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-top style="margin-top: 20px">
        <el-form-item label="审批备注">
          <el-input v-model="remark" type="textarea" :rows="3" placeholder="请输入审批备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirm">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";

const status = ref("");
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(5);
const dialogVisible = ref(false);
const remark = ref("");
const currentRow = ref<any>(null);
const actionType = ref<"approve" | "reject">("approve");

const tableData = ref([
  { applicant: "张三", dept: "技术部", type: "事假", timeRange: "2026-05-20 09:00 ~ 2026-05-21 18:00", days: 2, reason: "处理私事", status: "待审批" },
  { applicant: "李四", dept: "市场部", type: "病假", timeRange: "2026-05-18 09:00 ~ 2026-05-18 18:00", days: 1, reason: "身体不适", status: "待审批" },
  { applicant: "王五", dept: "人事部", type: "年假", timeRange: "2026-06-01 09:00 ~ 2026-06-05 18:00", days: 5, reason: "休年假旅行", status: "已通过" },
  { applicant: "赵六", dept: "财务部", type: "丧假", timeRange: "2026-05-15 09:00 ~ 2026-05-17 18:00", days: 3, reason: "家中有事", status: "已拒绝" }
]);

const getStatusType = (status: string) => {
  const map: Record<string, string> = {
    "待审批": "warning",
    "已通过": "success",
    "已拒绝": "danger"
  };
  return map[status] || "info";
};

const handleApprove = (row: any) => {
  currentRow.value = row;
  actionType.value = "approve";
  remark.value = "";
  dialogVisible.value = true;
};

const handleReject = (row: any) => {
  currentRow.value = row;
  actionType.value = "reject";
  remark.value = "";
  dialogVisible.value = true;
};

const handleConfirm = () => {
  const action = actionType.value === "approve" ? "通过" : "拒绝";
  ElMessage.success(`已${action}`);
  dialogVisible.value = false;
  currentRow.value.status = actionType.value === "approve" ? "已通过" : "已拒绝";
};
</script>

<style scoped lang="scss">
.leave-approval-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>