<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[var(--oa-text)]">员工档案扩展</span>
          <div class="flex gap-2">
            <el-input-number v-model="empId" :min="1" controls-position="right" />
            <el-button @click="reload">查询</el-button>
            <el-button type="primary" @click="openDialog">新增记录</el-button>
          </div>
        </div>
      </template>
      <el-tabs v-model="activeTab" @tab-change="reload">
        <el-tab-pane label="合同" name="contracts" />
        <el-tab-pane label="异动" name="changes" />
        <el-tab-pane label="证书" name="certificates" />
        <el-tab-pane label="教育经历" name="educations" />
      </el-tabs>
      <el-table :data="records" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="contractNo" label="合同号" min-width="140" />
        <el-table-column prop="changeType" label="异动类型" min-width="120" />
        <el-table-column prop="certificateName" label="证书" min-width="140" />
        <el-table-column prop="schoolName" label="学校" min-width="140" />
        <el-table-column prop="status" label="状态" width="110" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
      </el-table>
    </el-card>
    <el-dialog v-model="visible" title="新增档案扩展记录" width="560px">
      <el-form :model="form" label-width="100px">
        <template v-if="activeTab === 'contracts'">
          <el-form-item label="合同号"><el-input v-model="form.contractNo" /></el-form-item>
          <el-form-item label="合同类型"><el-input v-model="form.contractType" /></el-form-item>
          <el-form-item label="开始日期"><el-date-picker v-model="form.startDate" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
          <el-form-item label="结束日期"><el-date-picker v-model="form.endDate" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        </template>
        <template v-else-if="activeTab === 'changes'">
          <el-form-item label="异动类型"><el-input v-model="form.changeType" /></el-form-item>
          <el-form-item label="前部门ID"><el-input-number v-model="form.beforeDeptId" style="width: 100%" /></el-form-item>
          <el-form-item label="后部门ID"><el-input-number v-model="form.afterDeptId" style="width: 100%" /></el-form-item>
          <el-form-item label="原因"><el-input v-model="form.reason" type="textarea" /></el-form-item>
        </template>
        <template v-else-if="activeTab === 'certificates'">
          <el-form-item label="证书名称"><el-input v-model="form.certificateName" /></el-form-item>
          <el-form-item label="证书编号"><el-input v-model="form.certificateNo" /></el-form-item>
          <el-form-item label="签发机构"><el-input v-model="form.issueOrg" /></el-form-item>
        </template>
        <template v-else>
          <el-form-item label="学校"><el-input v-model="form.schoolName" /></el-form-item>
          <el-form-item label="专业"><el-input v-model="form.major" /></el-form-item>
          <el-form-item label="学历"><el-input v-model="form.degree" /></el-form-item>
        </template>
      </el-form>
      <template #footer><el-button @click="visible = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { employeeExtraApi } from "@/api/businessModules";

const empId = ref(1);
const activeTab = ref("contracts");
const loading = ref(false);
const records = ref<any[]>([]);
function rows(data: any) { return data?.records || data?.list || []; }
async function reload() {
  loading.value = true;
  try {
    const params = { pn: 1, ps: 20 };
    const res: any = activeTab.value === "changes" ? await employeeExtraApi.changes(empId.value, params) : activeTab.value === "certificates" ? await employeeExtraApi.certificates(empId.value, params) : activeTab.value === "educations" ? await employeeExtraApi.educations(empId.value, params) : await employeeExtraApi.contracts(empId.value, params);
    records.value = rows(res.data);
  } finally { loading.value = false; }
}
const visible = ref(false);
const form = reactive<any>({});
function openDialog() { Object.keys(form).forEach(key => delete form[key]); visible.value = true; }
async function save() {
  if (activeTab.value === "changes") await employeeExtraApi.createChange(empId.value, form);
  else if (activeTab.value === "certificates") await employeeExtraApi.createCertificate(empId.value, form);
  else if (activeTab.value === "educations") await employeeExtraApi.createEducation(empId.value, form);
  else await employeeExtraApi.createContract(empId.value, form);
  ElMessage.success("保存成功");
  visible.value = false;
  reload();
}
onMounted(reload);
</script>
