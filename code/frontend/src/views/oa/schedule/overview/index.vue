<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage } from "element-plus";
import { getScheduleList, getScheduleByDateRange } from "@/api/oa/schedule";

defineOptions({ name: "OaScheduleOverview" });

/** 日程记录 */
interface ScheduleRecord {
  id: number;
  title: string;
  date: string;
  time: string;
  content: string;
  userName: string;
  departmentName: string;
  departmentId: number;
}

/** 部门选项 */
interface Department {
  id: number;
  name: string;
}

const loading = ref(false);
const schedules = ref<ScheduleRecord[]>([]);
const departments = ref<Department[]>([]);

const queryParams = reactive({
  departmentId: undefined as number | undefined,
  dateRange: [] as string[]
});

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
});

/** 加载部门列表 */
async function fetchDepartments() {
  try {
    const res = await getScheduleList({ type: "departments" });
    departments.value = res.data ?? res ?? [];
  } catch {
    // 部门列表加载失败时使用默认数据
    departments.value = [
      { id: 1, name: "技术部" },
      { id: 2, name: "市场部" },
      { id: 3, name: "人事部" },
      { id: 4, name: "财务部" }
    ];
  }
}

/** 加载日程列表 */
async function fetchSchedules() {
  loading.value = true;
  try {
    const params: any = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    };
    if (queryParams.departmentId) {
      params.departmentId = queryParams.departmentId;
    }
    if (queryParams.dateRange && queryParams.dateRange.length === 2) {
      params.startDate = queryParams.dateRange[0];
      params.endDate = queryParams.dateRange[1];
    }

    let res;
    if (queryParams.dateRange && queryParams.dateRange.length === 2) {
      res = await getScheduleByDateRange(queryParams.dateRange[0], queryParams.dateRange[1]);
    } else {
      res = await getScheduleList(params);
    }

    const data = res.data ?? res;
    schedules.value = data.list ?? data.records ?? data ?? [];
    pagination.total = data.total ?? schedules.value.length;
  } catch {
    ElMessage.error("获取日程列表失败");
  } finally {
    loading.value = false;
  }
}

/** 搜索 */
function handleSearch() {
  pagination.pageNum = 1;
  fetchSchedules();
}

/** 重置 */
function handleReset() {
  queryParams.departmentId = undefined;
  queryParams.dateRange = [];
  pagination.pageNum = 1;
  fetchSchedules();
}

/** 分页变化 */
function handlePageChange(pageNum: number) {
  pagination.pageNum = pageNum;
  fetchSchedules();
}

function handleSizeChange(pageSize: number) {
  pagination.pageSize = pageSize;
  pagination.pageNum = 1;
  fetchSchedules();
}

onMounted(() => {
  fetchDepartments();
  fetchSchedules();
});
</script>

<template>
  <div class="oa-schedule-overview">
    <el-card shadow="hover">
      <template #header>
        <span class="card-title">日程总览</span>
      </template>

      <!-- 搜索条件 -->
      <el-form :inline="true" class="search-form">
        <el-form-item label="部门">
          <el-select
            v-model="queryParams.departmentId"
            placeholder="请选择部门"
            clearable
            style="width: 200px"
          >
            <el-option
              v-for="dept in departments"
              :key="dept.id"
              :label="dept.name"
              :value="dept.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="queryParams.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 300px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 日程表格 -->
      <el-table
        :data="schedules"
        stripe
        v-loading="loading"
        empty-text="暂无日程数据"
        style="width: 100%"
      >
        <el-table-column prop="userName" label="员工姓名" width="120" align="center" />
        <el-table-column prop="departmentName" label="所属部门" width="120" align="center" />
        <el-table-column prop="title" label="日程标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="date" label="日期" width="120" align="center" />
        <el-table-column prop="time" label="时间" width="100" align="center" />
        <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.oa-schedule-overview {
  padding: 20px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.search-form {
  margin-bottom: 16px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
