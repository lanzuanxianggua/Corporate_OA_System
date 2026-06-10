<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[var(--oa-text)]">绩效管理</span>
          <div class="flex gap-2">
            <el-button type="primary" @click="openGoalDialog">新增目标</el-button>
            <el-button type="success" @click="openEvalDialog">新增评估</el-button>
            <el-button @click="generateResults">生成结果</el-button>
          </div>
        </div>
      </template>
      <el-tabs v-model="activeTab" @tab-change="reload">
        <el-tab-pane label="目标" name="goals">
          <el-table :data="goals" v-loading="loading" stripe>
            <el-table-column prop="cycleId" label="周期ID" width="100" />
            <el-table-column prop="empId" label="员工ID" width="100" />
            <el-table-column prop="goalContent" label="目标内容" min-width="220" />
            <el-table-column prop="targetValue" label="目标值" width="140" />
            <el-table-column prop="weight" label="权重" width="90" />
            <el-table-column prop="score" label="得分" width="90" />
            <el-table-column prop="grade" label="等级" width="80" />
            <el-table-column prop="status" label="状态" width="110" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button link type="primary" @click="submitGoal(row.id)">提交</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="评估" name="evals">
          <el-table :data="evals" v-loading="loading" stripe>
            <el-table-column prop="goalId" label="目标ID" width="100" />
            <el-table-column prop="evaluatorId" label="评估人" width="100" />
            <el-table-column prop="evalType" label="类型" width="100" />
            <el-table-column prop="score" label="得分" width="100" />
            <el-table-column prop="comment" label="评语" min-width="220" />
            <el-table-column prop="status" label="状态" width="110" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button link type="primary" @click="submitEval(row.id)">提交</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="结果" name="results">
          <el-table :data="results" v-loading="loading" stripe>
            <el-table-column prop="cycleId" label="周期ID" width="100" />
            <el-table-column prop="empId" label="员工ID" width="100" />
            <el-table-column prop="totalScore" label="总分" width="100" />
            <el-table-column prop="grade" label="等级" width="90" />
            <el-table-column prop="ranking" label="排名" width="90" />
            <el-table-column prop="status" label="状态" width="120" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
      <div class="mt-4 flex justify-end">
        <OaPagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" @change="reload"  :page-sizes="[10, 20, 50]" />
      </div>
    </el-card>

    <el-dialog v-model="goalVisible" title="新增绩效目标" width="560px">
      <el-form :model="goalForm" label-width="90px">
        <el-form-item label="周期ID"><el-input-number v-model="goalForm.cycleId" style="width: 100%" /></el-form-item>
        <el-form-item label="员工ID"><el-input-number v-model="goalForm.empId" style="width: 100%" /></el-form-item>
        <el-form-item label="目标内容"><el-input v-model="goalForm.goalContent" type="textarea" /></el-form-item>
        <el-form-item label="目标值"><el-input v-model="goalForm.targetValue" /></el-form-item>
        <el-form-item label="权重"><el-input-number v-model="goalForm.weight" :min="0" :max="100" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="goalVisible = false">取消</el-button><el-button type="primary" @click="saveGoal">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="evalVisible" title="新增绩效评估" width="560px">
      <el-form :model="evalForm" label-width="90px">
        <el-form-item label="目标ID"><el-input-number v-model="evalForm.goalId" style="width: 100%" /></el-form-item>
        <el-form-item label="评估人ID"><el-input-number v-model="evalForm.evaluatorId" style="width: 100%" /></el-form-item>
        <el-form-item label="类型"><el-select v-model="evalForm.evalType" style="width: 100%"><el-option label="自评" value="SELF" /><el-option label="主管评" value="MANAGER" /></el-select></el-form-item>
        <el-form-item label="得分"><el-input-number v-model="evalForm.score" :min="0" :max="100" style="width: 100%" /></el-form-item>
        <el-form-item label="评语"><el-input v-model="evalForm.comment" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="evalVisible = false">取消</el-button><el-button type="primary" @click="saveEval">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { performanceApi } from "@/api/businessModules";

const activeTab = ref("goals");
const loading = ref(false);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const goals = ref<any[]>([]);
const evals = ref<any[]>([]);
const results = ref<any[]>([]);
const cycleId = ref(1);

function rows(data: any) { return data?.records || data?.list || []; }
async function reload() {
  loading.value = true;
  try {
    const params = { pn: pageNum.value, ps: pageSize.value };
    const res: any = activeTab.value === "evals" ? await performanceApi.evals(params) : activeTab.value === "results" ? await performanceApi.results(params) : await performanceApi.goals(params);
    if (activeTab.value === "evals") evals.value = rows(res.data);
    else if (activeTab.value === "results") results.value = rows(res.data);
    else goals.value = rows(res.data);
    total.value = res.data?.total || 0;
  } finally {
    loading.value = false;
  }
}

const goalVisible = ref(false);
const goalForm = reactive<any>({ cycleId: 1, empId: undefined, goalContent: "", targetValue: "", weight: 100 });
function openGoalDialog() { Object.assign(goalForm, { cycleId: 1, empId: undefined, goalContent: "", targetValue: "", weight: 100 }); goalVisible.value = true; }
async function saveGoal() { await performanceApi.createGoal(goalForm); ElMessage.success("目标已保存"); goalVisible.value = false; reload(); }
async function submitGoal(id: number) { await performanceApi.submitGoal(id); ElMessage.success("目标已提交"); reload(); }

const evalVisible = ref(false);
const evalForm = reactive<any>({ goalId: undefined, evaluatorId: undefined, evalType: "SELF", score: 0, comment: "" });
function openEvalDialog() { Object.assign(evalForm, { goalId: undefined, evaluatorId: undefined, evalType: "SELF", score: 0, comment: "" }); evalVisible.value = true; }
async function saveEval() { await performanceApi.createEval(evalForm); ElMessage.success("评估已保存"); evalVisible.value = false; reload(); }
async function submitEval(id: number) { await performanceApi.submitEval(id); ElMessage.success("评估已提交"); reload(); }
async function generateResults() { await performanceApi.generateResults(cycleId.value); ElMessage.success("结果已生成"); activeTab.value = "results"; reload(); }

onMounted(reload);
</script>
