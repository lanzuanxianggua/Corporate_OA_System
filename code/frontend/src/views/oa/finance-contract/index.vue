<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[var(--oa-text)]">财务合同与付款</span>
          <div class="flex gap-2">
            <el-button type="primary" @click="openContract">新增合同</el-button>
            <el-button type="success" @click="openPayment">新增付款</el-button>
          </div>
        </div>
      </template>
      <el-tabs v-model="activeTab" @tab-change="reload">
        <el-tab-pane label="合同" name="contracts">
          <el-table :data="contracts" v-loading="loading" stripe>
            <el-table-column prop="contractNo" label="合同号" width="150" />
            <el-table-column prop="contractName" label="合同名称" min-width="180" />
            <el-table-column prop="counterparty" label="相对方" min-width="160" />
            <el-table-column prop="amount" label="金额" width="120" />
            <el-table-column prop="deptId" label="部门ID" width="100" />
            <el-table-column prop="status" label="状态" width="110" />
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button link type="success" @click="activateContract(row.id)">生效</el-button>
                <el-button link @click="closeContract(row.id)">关闭</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="付款" name="payments">
          <el-table :data="payments" v-loading="loading" stripe>
            <el-table-column prop="paymentNo" label="付款单号" width="160" />
            <el-table-column prop="contractId" label="合同ID" width="100" />
            <el-table-column prop="payee" label="收款方" min-width="160" />
            <el-table-column prop="amount" label="金额" width="120" />
            <el-table-column prop="plannedDate" label="计划日期" width="130" />
            <el-table-column prop="paidTime" label="付款时间" width="180" />
            <el-table-column prop="status" label="状态" width="110" />
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button link type="primary" @click="submitPayment(row.id)">提交</el-button>
                <el-button link type="success" @click="markPaid(row.id)">支付</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="contractVisible" title="新增财务合同" width="560px">
      <el-form :model="contractForm" label-width="100px">
        <el-form-item label="合同号"><el-input v-model="contractForm.contractNo" /></el-form-item>
        <el-form-item label="合同名称"><el-input v-model="contractForm.contractName" /></el-form-item>
        <el-form-item label="相对方"><el-input v-model="contractForm.counterparty" /></el-form-item>
        <el-form-item label="金额"><el-input-number v-model="contractForm.amount" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="部门ID"><el-input-number v-model="contractForm.deptId" style="width: 100%" /></el-form-item>
        <el-form-item label="签署日期"><el-date-picker v-model="contractForm.signedDate" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="contractVisible = false">取消</el-button><el-button type="primary" @click="saveContract">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="paymentVisible" title="新增付款" width="560px">
      <el-form :model="paymentForm" label-width="100px">
        <el-form-item label="合同ID"><el-input-number v-model="paymentForm.contractId" style="width: 100%" /></el-form-item>
        <el-form-item label="收款方"><el-input v-model="paymentForm.payee" /></el-form-item>
        <el-form-item label="金额"><el-input-number v-model="paymentForm.amount" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="计划日期"><el-date-picker v-model="paymentForm.plannedDate" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="paymentForm.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="paymentVisible = false">取消</el-button><el-button type="primary" @click="savePayment">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { financeContractApi } from "@/api/businessModules";

const activeTab = ref("contracts");
const loading = ref(false);
const contracts = ref<any[]>([]);
const payments = ref<any[]>([]);
function rows(data: any) { return data?.records || data?.list || []; }
async function reload() {
  loading.value = true;
  try {
    const res: any = activeTab.value === "payments" ? await financeContractApi.payments({ pn: 1, ps: 30 }) : await financeContractApi.contracts({ pn: 1, ps: 30 });
    if (activeTab.value === "payments") payments.value = rows(res.data);
    else contracts.value = rows(res.data);
  } finally { loading.value = false; }
}
const contractVisible = ref(false);
const contractForm = reactive<any>({ contractNo: "", contractName: "", counterparty: "", amount: 0, deptId: undefined, signedDate: "" });
function openContract() { Object.assign(contractForm, { contractNo: "", contractName: "", counterparty: "", amount: 0, deptId: undefined, signedDate: "" }); contractVisible.value = true; }
async function saveContract() { await financeContractApi.createContract(contractForm); ElMessage.success("合同已保存"); contractVisible.value = false; reload(); }
async function activateContract(id: number) { await financeContractApi.activateContract(id); ElMessage.success("合同已生效"); reload(); }
async function closeContract(id: number) { await financeContractApi.closeContract(id); ElMessage.success("合同已关闭"); reload(); }
const paymentVisible = ref(false);
const paymentForm = reactive<any>({ contractId: undefined, payee: "", amount: 0, plannedDate: "", remark: "" });
function openPayment() { Object.assign(paymentForm, { contractId: undefined, payee: "", amount: 0, plannedDate: "", remark: "" }); paymentVisible.value = true; }
async function savePayment() { await financeContractApi.createPayment(paymentForm); ElMessage.success("付款已保存"); paymentVisible.value = false; activeTab.value = "payments"; reload(); }
async function submitPayment(id: number) { await financeContractApi.submitPayment(id); ElMessage.success("付款已提交"); reload(); }
async function markPaid(id: number) {
  const result = await ElMessageBox.prompt("请输入支付方式", "确认支付", { inputValue: "BANK" });
  await financeContractApi.markPaid(id, result.value);
  ElMessage.success("已标记支付");
  reload();
}
onMounted(reload);
</script>
