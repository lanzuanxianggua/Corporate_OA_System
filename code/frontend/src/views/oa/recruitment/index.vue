<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[var(--oa-text)]">招聘管理</span>
          <div class="flex gap-2">
            <el-button type="primary" @click="openCandidateDialog">新增候选人</el-button>
            <el-button type="success" @click="openInterviewDialog">安排面试</el-button>
            <el-button @click="openOfferDialog">发 Offer</el-button>
          </div>
        </div>
      </template>
      <el-tabs v-model="activeTab" @tab-change="reload">
        <el-tab-pane label="候选人" name="candidates">
          <el-table :data="candidates" v-loading="loading" stripe>
            <el-table-column prop="jobId" label="岗位ID" width="100" />
            <el-table-column prop="name" label="姓名" width="120" />
            <el-table-column prop="phone" label="电话" width="140" />
            <el-table-column prop="email" label="邮箱" min-width="180" />
            <el-table-column prop="source" label="来源" width="100" />
            <el-table-column prop="interviewScore" label="面试分" width="100" />
            <el-table-column prop="status" label="状态" width="120" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="面试" name="interviews">
          <el-table :data="interviews" v-loading="loading" stripe>
            <el-table-column prop="candidateId" label="候选人ID" width="110" />
            <el-table-column prop="round" label="轮次" width="80" />
            <el-table-column prop="interviewerId" label="面试官" width="110" />
            <el-table-column prop="score" label="得分" width="90" />
            <el-table-column prop="result" label="结果" width="100" />
            <el-table-column prop="evaluation" label="评价" min-width="220" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="Offer" name="offers">
          <el-table :data="offers" v-loading="loading" stripe>
            <el-table-column prop="candidateId" label="候选人ID" width="110" />
            <el-table-column prop="offerSalary" label="薪资" width="120" />
            <el-table-column prop="offerDate" label="Offer日期" width="130" />
            <el-table-column prop="onboardDate" label="入职日期" width="130" />
            <el-table-column prop="status" label="状态" width="120" />
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button link type="success" @click="accept(row.id)">接受</el-button>
                <el-button link type="primary" @click="onboard(row.id)">入职</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="candidateVisible" title="新增候选人" width="520px">
      <el-form :model="candidateForm" label-width="90px">
        <el-form-item label="岗位ID"><el-input-number v-model="candidateForm.jobId" style="width: 100%" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="candidateForm.name" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="candidateForm.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="candidateForm.email" /></el-form-item>
        <el-form-item label="来源"><el-input v-model="candidateForm.source" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="candidateVisible = false">取消</el-button><el-button type="primary" @click="saveCandidate">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="interviewVisible" title="安排面试" width="520px">
      <el-form :model="interviewForm" label-width="90px">
        <el-form-item label="候选人ID"><el-input-number v-model="interviewForm.candidateId" style="width: 100%" /></el-form-item>
        <el-form-item label="轮次"><el-input-number v-model="interviewForm.round" :min="1" style="width: 100%" /></el-form-item>
        <el-form-item label="面试官ID"><el-input-number v-model="interviewForm.interviewerId" style="width: 100%" /></el-form-item>
        <el-form-item label="得分"><el-input-number v-model="interviewForm.score" :min="0" :max="100" style="width: 100%" /></el-form-item>
        <el-form-item label="结果"><el-select v-model="interviewForm.result" style="width: 100%"><el-option label="通过" value="PASS" /><el-option label="未通过" value="FAIL" /></el-select></el-form-item>
        <el-form-item label="评价"><el-input v-model="interviewForm.evaluation" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="interviewVisible = false">取消</el-button><el-button type="primary" @click="saveInterview">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="offerVisible" title="发 Offer" width="520px">
      <el-form :model="offerForm" label-width="90px">
        <el-form-item label="候选人ID"><el-input-number v-model="offerForm.candidateId" style="width: 100%" /></el-form-item>
        <el-form-item label="薪资"><el-input-number v-model="offerForm.offerSalary" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="Offer日期"><el-date-picker v-model="offerForm.offerDate" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="入职日期"><el-date-picker v-model="offerForm.onboardDate" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="offerVisible = false">取消</el-button><el-button type="primary" @click="saveOffer">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { recruitmentApi } from "@/api/businessModules";

const activeTab = ref("candidates");
const loading = ref(false);
const candidates = ref<any[]>([]);
const interviews = ref<any[]>([]);
const offers = ref<any[]>([]);
function rows(data: any) { return data?.records || data?.list || []; }
async function reload() {
  loading.value = true;
  try {
    const res: any = activeTab.value === "interviews" ? await recruitmentApi.interviews({ pn: 1, ps: 20 }) : activeTab.value === "offers" ? await recruitmentApi.offers({ pn: 1, ps: 20 }) : await recruitmentApi.candidates({ pn: 1, ps: 20 });
    if (activeTab.value === "interviews") interviews.value = rows(res.data);
    else if (activeTab.value === "offers") offers.value = rows(res.data);
    else candidates.value = rows(res.data);
  } finally { loading.value = false; }
}
const candidateVisible = ref(false);
const candidateForm = reactive<any>({ jobId: undefined, name: "", phone: "", email: "", source: "" });
function openCandidateDialog() { Object.assign(candidateForm, { jobId: undefined, name: "", phone: "", email: "", source: "" }); candidateVisible.value = true; }
async function saveCandidate() { await recruitmentApi.createCandidate(candidateForm); ElMessage.success("候选人已保存"); candidateVisible.value = false; reload(); }
const interviewVisible = ref(false);
const interviewForm = reactive<any>({ candidateId: undefined, round: 1, interviewerId: undefined, score: 0, result: "PASS", evaluation: "" });
function openInterviewDialog() { Object.assign(interviewForm, { candidateId: undefined, round: 1, interviewerId: undefined, score: 0, result: "PASS", evaluation: "" }); interviewVisible.value = true; }
async function saveInterview() { await recruitmentApi.createInterview(interviewForm); ElMessage.success("面试已保存"); interviewVisible.value = false; activeTab.value = "interviews"; reload(); }
const offerVisible = ref(false);
const offerForm = reactive<any>({ candidateId: undefined, offerSalary: 0, offerDate: "", onboardDate: "" });
function openOfferDialog() { Object.assign(offerForm, { candidateId: undefined, offerSalary: 0, offerDate: "", onboardDate: "" }); offerVisible.value = true; }
async function saveOffer() { await recruitmentApi.createOffer(offerForm); ElMessage.success("Offer 已保存"); offerVisible.value = false; activeTab.value = "offers"; reload(); }
async function accept(id: number) { await recruitmentApi.acceptOffer(id); ElMessage.success("Offer 已接受"); reload(); }
async function onboard(id: number) { await recruitmentApi.onboardOffer(id); ElMessage.success("已标记入职"); reload(); }
onMounted(reload);
</script>
