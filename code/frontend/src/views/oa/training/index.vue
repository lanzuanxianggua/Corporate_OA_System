<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[#303133]">培训管理</span>
          <div class="flex gap-2">
            <el-button type="primary" @click="openCourse">新增课程</el-button>
            <el-button type="success" @click="openPlan">新增计划</el-button>
            <el-button @click="openSession">新增班级</el-button>
            <el-button @click="openEnroll">报名</el-button>
          </div>
        </div>
      </template>
      <el-tabs v-model="activeTab" @tab-change="reload">
        <el-tab-pane label="课程" name="courses">
          <el-table :data="courses" v-loading="loading" stripe>
            <el-table-column prop="courseName" label="课程名称" min-width="180" />
            <el-table-column prop="courseType" label="类型" width="120" />
            <el-table-column prop="credit" label="学分" width="90" />
            <el-table-column prop="totalHours" label="课时" width="90" />
            <el-table-column prop="status" label="状态" width="110" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="计划" name="plans">
          <el-table :data="plans" v-loading="loading" stripe>
            <el-table-column prop="planName" label="计划名称" min-width="180" />
            <el-table-column prop="year" label="年度" width="90" />
            <el-table-column prop="courseId" label="课程ID" width="100" />
            <el-table-column prop="totalBudget" label="预算" width="120" />
            <el-table-column prop="status" label="状态" width="110" />
            <el-table-column label="操作" width="100"><template #default="{ row }"><el-button link type="primary" @click="publishPlan(row.id)">发布</el-button></template></el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="班级" name="sessions">
          <el-table :data="sessions" v-loading="loading" stripe>
            <el-table-column prop="sessionName" label="班级" min-width="180" />
            <el-table-column prop="planId" label="计划ID" width="100" />
            <el-table-column prop="location" label="地点" width="140" />
            <el-table-column prop="trainer" label="讲师" width="120" />
            <el-table-column prop="enrolledNum" label="报名数" width="90" />
            <el-table-column prop="status" label="状态" width="110" />
            <el-table-column label="操作" width="140"><template #default="{ row }"><el-button link type="success" @click="startSession(row.id)">开放</el-button><el-button link @click="closeSession(row.id)">关闭</el-button></template></el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="报名" name="enrollments">
          <el-table :data="enrollments" v-loading="loading" stripe>
            <el-table-column prop="sessionId" label="班级ID" width="100" />
            <el-table-column prop="empId" label="员工ID" width="100" />
            <el-table-column prop="attendance" label="签到" width="110" />
            <el-table-column prop="score" label="成绩" width="90" />
            <el-table-column prop="creditGranted" label="学分" width="90" />
            <el-table-column label="操作" width="140"><template #default="{ row }"><el-button link type="success" @click="signIn(row.id)">签到</el-button><el-button link @click="score(row.id)">评分</el-button></template></el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="学分记录" name="records">
          <el-table :data="records" v-loading="loading" stripe>
            <el-table-column prop="empId" label="员工ID" width="100" />
            <el-table-column prop="courseId" label="课程ID" width="100" />
            <el-table-column prop="sessionId" label="班级ID" width="100" />
            <el-table-column prop="totalCredit" label="学分" width="100" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="courseVisible" title="新增课程" width="520px">
      <el-form :model="courseForm" label-width="90px">
        <el-form-item label="课程名称"><el-input v-model="courseForm.courseName" /></el-form-item>
        <el-form-item label="类型"><el-input v-model="courseForm.courseType" /></el-form-item>
        <el-form-item label="学分"><el-input-number v-model="courseForm.credit" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="课时"><el-input-number v-model="courseForm.totalHours" :min="0" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="courseVisible = false">取消</el-button><el-button type="primary" @click="saveCourse">保存</el-button></template>
    </el-dialog>
    <el-dialog v-model="planVisible" title="新增计划" width="520px">
      <el-form :model="planForm" label-width="90px">
        <el-form-item label="计划名称"><el-input v-model="planForm.planName" /></el-form-item>
        <el-form-item label="年度"><el-input-number v-model="planForm.year" style="width: 100%" /></el-form-item>
        <el-form-item label="课程ID"><el-input-number v-model="planForm.courseId" style="width: 100%" /></el-form-item>
        <el-form-item label="预算"><el-input-number v-model="planForm.totalBudget" :min="0" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="planVisible = false">取消</el-button><el-button type="primary" @click="savePlan">保存</el-button></template>
    </el-dialog>
    <el-dialog v-model="sessionVisible" title="新增班级" width="520px">
      <el-form :model="sessionForm" label-width="90px">
        <el-form-item label="计划ID"><el-input-number v-model="sessionForm.planId" style="width: 100%" /></el-form-item>
        <el-form-item label="班级名称"><el-input v-model="sessionForm.sessionName" /></el-form-item>
        <el-form-item label="地点"><el-input v-model="sessionForm.location" /></el-form-item>
        <el-form-item label="讲师"><el-input v-model="sessionForm.trainer" /></el-form-item>
        <el-form-item label="容量"><el-input-number v-model="sessionForm.maxCapacity" :min="1" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="sessionVisible = false">取消</el-button><el-button type="primary" @click="saveSession">保存</el-button></template>
    </el-dialog>
    <el-dialog v-model="enrollVisible" title="培训报名" width="420px">
      <el-form :model="enrollForm" label-width="90px">
        <el-form-item label="班级ID"><el-input-number v-model="enrollForm.sessionId" style="width: 100%" /></el-form-item>
        <el-form-item label="员工ID"><el-input-number v-model="enrollForm.empId" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="enrollVisible = false">取消</el-button><el-button type="primary" @click="saveEnroll">报名</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { trainingApi } from "@/api/businessModules";

const activeTab = ref("courses");
const loading = ref(false);
const courses = ref<any[]>([]);
const plans = ref<any[]>([]);
const sessions = ref<any[]>([]);
const enrollments = ref<any[]>([]);
const records = ref<any[]>([]);
function rows(data: any) { return data?.records || data?.list || []; }
async function reload() {
  loading.value = true;
  try {
    const params = { pn: 1, ps: 30 };
    const res: any = activeTab.value === "plans" ? await trainingApi.plans(params) : activeTab.value === "sessions" ? await trainingApi.sessions(params) : activeTab.value === "enrollments" ? await trainingApi.enrollments(params) : activeTab.value === "records" ? await trainingApi.records(params) : await trainingApi.courses(params);
    if (activeTab.value === "plans") plans.value = rows(res.data);
    else if (activeTab.value === "sessions") sessions.value = rows(res.data);
    else if (activeTab.value === "enrollments") enrollments.value = rows(res.data);
    else if (activeTab.value === "records") records.value = rows(res.data);
    else courses.value = rows(res.data);
  } finally { loading.value = false; }
}
const courseVisible = ref(false);
const courseForm = reactive<any>({ courseName: "", courseType: "", credit: 0, totalHours: 0 });
function openCourse() { Object.assign(courseForm, { courseName: "", courseType: "", credit: 0, totalHours: 0 }); courseVisible.value = true; }
async function saveCourse() { await trainingApi.createCourse(courseForm); ElMessage.success("课程已保存"); courseVisible.value = false; reload(); }
const planVisible = ref(false);
const planForm = reactive<any>({ planName: "", year: new Date().getFullYear(), courseId: undefined, totalBudget: 0 });
function openPlan() { Object.assign(planForm, { planName: "", year: new Date().getFullYear(), courseId: undefined, totalBudget: 0 }); planVisible.value = true; }
async function savePlan() { await trainingApi.createPlan(planForm); ElMessage.success("计划已保存"); planVisible.value = false; activeTab.value = "plans"; reload(); }
async function publishPlan(id: number) { await trainingApi.publishPlan(id); ElMessage.success("计划已发布"); reload(); }
const sessionVisible = ref(false);
const sessionForm = reactive<any>({ planId: undefined, sessionName: "", location: "", trainer: "", maxCapacity: 30 });
function openSession() { Object.assign(sessionForm, { planId: undefined, sessionName: "", location: "", trainer: "", maxCapacity: 30 }); sessionVisible.value = true; }
async function saveSession() { await trainingApi.createSession(sessionForm); ElMessage.success("班级已保存"); sessionVisible.value = false; activeTab.value = "sessions"; reload(); }
async function startSession(id: number) { await trainingApi.startSession(id); ElMessage.success("班级已开放"); reload(); }
async function closeSession(id: number) { await trainingApi.closeSession(id); ElMessage.success("班级已关闭"); reload(); }
const enrollVisible = ref(false);
const enrollForm = reactive<any>({ sessionId: undefined, empId: undefined });
function openEnroll() { Object.assign(enrollForm, { sessionId: undefined, empId: undefined }); enrollVisible.value = true; }
async function saveEnroll() { await trainingApi.enroll(enrollForm); ElMessage.success("报名成功"); enrollVisible.value = false; activeTab.value = "enrollments"; reload(); }
async function signIn(id: number) { await trainingApi.signIn(id); ElMessage.success("签到成功"); reload(); }
async function score(id: number) { const value = await ElMessageBox.prompt("请输入成绩", "评分", { inputValue: "90" }); await trainingApi.score(id, Number(value.value)); ElMessage.success("成绩已保存"); reload(); }
onMounted(reload);
</script>
