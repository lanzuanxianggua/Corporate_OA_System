<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[var(--oa-text)]">办公用品管理</span>
          <div class="flex gap-2">
            <el-button @click="openCategoryDialog">新增分类</el-button>
            <el-button type="primary" @click="openSupplyDialog">新增用品</el-button>
            <el-button type="success" @click="openRequestDialog">领用/入库</el-button>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="reload">
        <el-tab-pane label="用品库存" name="supplies">
          <el-table :data="supplies" v-loading="loading" stripe>
            <el-table-column prop="supplyCode" label="编码" width="140" />
            <el-table-column prop="supplyName" label="用品名称" min-width="160" />
            <el-table-column prop="unit" label="单位" width="80" />
            <el-table-column prop="spec" label="规格" min-width="120" />
            <el-table-column prop="safetyStock" label="安全库存" width="100" />
            <el-table-column prop="status" label="状态" width="100" />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openSupplyDialog(row)">编辑</el-button>
                <el-button link type="success" @click="openStockDialog(row)">调库存</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="审批单" name="requests">
          <el-table :data="requests" v-loading="loading" stripe>
            <el-table-column prop="requestNo" label="单号" width="160" />
            <el-table-column prop="requestType" label="类型" width="90" />
            <el-table-column prop="empId" label="员工ID" width="100" />
            <el-table-column prop="deptId" label="部门ID" width="100" />
            <el-table-column prop="reason" label="原因" min-width="180" />
            <el-table-column prop="status" label="状态" width="110" />
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button link type="success" :disabled="row.status !== 'PENDING'" @click="approve(row.id)">通过</el-button>
                <el-button link type="danger" :disabled="row.status !== 'PENDING'" @click="reject(row.id)">驳回</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>

      <div class="mt-4 flex justify-end">
        <OaPagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" @change="reload" />
      </div>
    </el-card>

    <el-dialog v-model="supplyVisible" :title="supplyForm.id ? '编辑用品' : '新增用品'" width="520px">
      <el-form :model="supplyForm" label-width="90px">
        <el-form-item label="名称"><el-input v-model="supplyForm.supplyName" /></el-form-item>
        <el-form-item label="编码"><el-input v-model="supplyForm.supplyCode" placeholder="留空自动生成" /></el-form-item>
        <el-form-item label="单位"><el-input v-model="supplyForm.unit" /></el-form-item>
        <el-form-item label="规格"><el-input v-model="supplyForm.spec" /></el-form-item>
        <el-form-item label="安全库存"><el-input-number v-model="supplyForm.safetyStock" :min="0" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="supplyVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSupply">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="categoryVisible" title="新增用品分类" width="420px">
      <el-form :model="categoryForm" label-width="90px">
        <el-form-item label="分类名称"><el-input v-model="categoryForm.categoryName" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCategory">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="stockVisible" title="库存调整" width="420px">
      <el-form :model="stockForm" label-width="90px">
        <el-form-item label="调整数量"><el-input-number v-model="stockForm.quantity" style="width: 100%" /></el-form-item>
        <el-form-item label="库位"><el-input v-model="stockForm.location" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockVisible = false">取消</el-button>
        <el-button type="primary" @click="adjustStock">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="requestVisible" title="用品领用/入库" width="560px">
      <el-form :model="requestForm" label-width="90px">
        <el-form-item label="类型">
          <el-select v-model="requestForm.request.requestType" style="width: 100%">
            <el-option label="领用" value="OUT" />
            <el-option label="入库" value="IN" />
          </el-select>
        </el-form-item>
        <el-form-item label="员工ID"><el-input-number v-model="requestForm.request.empId" style="width: 100%" /></el-form-item>
        <el-form-item label="部门ID"><el-input-number v-model="requestForm.request.deptId" style="width: 100%" /></el-form-item>
        <el-form-item label="用品ID"><el-input-number v-model="requestForm.items[0].supplyId" style="width: 100%" /></el-form-item>
        <el-form-item label="数量"><el-input-number v-model="requestForm.items[0].quantity" :min="1" style="width: 100%" /></el-form-item>
        <el-form-item label="原因"><el-input v-model="requestForm.request.reason" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="requestVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRequest">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { supplyApi } from "@/api/businessModules";

const activeTab = ref("supplies");
const loading = ref(false);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const supplies = ref<any[]>([]);
const requests = ref<any[]>([]);

function pageRows(data: any) {
  return data?.records || data?.list || [];
}

async function reload() {
  loading.value = true;
  try {
    if (activeTab.value === "requests") {
      const res: any = await supplyApi.requests({ pn: pageNum.value, ps: pageSize.value });
      requests.value = pageRows(res.data);
      total.value = res.data?.total || 0;
    } else {
      const res: any = await supplyApi.list({ pn: pageNum.value, ps: pageSize.value });
      supplies.value = pageRows(res.data);
      total.value = res.data?.total || 0;
    }
  } finally {
    loading.value = false;
  }
}

const supplyVisible = ref(false);
const supplyForm = reactive<any>({ id: undefined, supplyCode: "", supplyName: "", unit: "", spec: "", safetyStock: 0 });
function openSupplyDialog(row?: any) {
  Object.assign(supplyForm, row || { id: undefined, supplyCode: "", supplyName: "", unit: "", spec: "", safetyStock: 0 });
  supplyVisible.value = true;
}
async function saveSupply() {
  if (supplyForm.id) await supplyApi.update(supplyForm.id, supplyForm);
  else await supplyApi.create(supplyForm);
  ElMessage.success("保存成功");
  supplyVisible.value = false;
  reload();
}

const categoryVisible = ref(false);
const categoryForm = reactive({ categoryName: "" });
function openCategoryDialog() {
  categoryForm.categoryName = "";
  categoryVisible.value = true;
}
async function saveCategory() {
  await supplyApi.createCategory(categoryForm);
  ElMessage.success("分类已创建");
  categoryVisible.value = false;
}

const stockVisible = ref(false);
const stockForm = reactive({ supplyId: 0, quantity: 0, location: "" });
function openStockDialog(row: any) {
  Object.assign(stockForm, { supplyId: row.id, quantity: 0, location: "" });
  stockVisible.value = true;
}
async function adjustStock() {
  await supplyApi.adjustStock(stockForm.supplyId, stockForm);
  ElMessage.success("库存已调整");
  stockVisible.value = false;
  reload();
}

const requestVisible = ref(false);
const requestForm = reactive<any>({ request: { requestType: "OUT", empId: undefined, deptId: undefined, reason: "" }, items: [{ supplyId: undefined, quantity: 1 }] });
function openRequestDialog() {
  Object.assign(requestForm.request, { requestType: "OUT", empId: undefined, deptId: undefined, reason: "" });
  Object.assign(requestForm.items[0], { supplyId: undefined, quantity: 1 });
  requestVisible.value = true;
}
async function saveRequest() {
  await supplyApi.createRequest(requestForm);
  ElMessage.success("申请已提交");
  requestVisible.value = false;
  activeTab.value = "requests";
  reload();
}
async function approve(id: number) {
  await supplyApi.approveRequest(id);
  ElMessage.success("已通过");
  reload();
}
async function reject(id: number) {
  const reason = await ElMessageBox.prompt("请输入驳回原因", "驳回", { inputValue: "不符合领用规则" });
  await supplyApi.rejectRequest(id, reason.value);
  ElMessage.success("已驳回");
  reload();
}

onMounted(reload);
</script>
