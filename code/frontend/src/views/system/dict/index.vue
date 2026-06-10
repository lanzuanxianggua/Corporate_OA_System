<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <span class="text-base font-semibold text-[var(--oa-text)]">字典管理</span>
      </template>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="字典类型" name="type">
          <div class="mb-4 flex justify-between">
            <el-input v-model="typeName" placeholder="搜索字典名称/类型" style="width: 280px" clearable :prefix-icon="Search" @clear="fetchTypeList" @keyup.enter="fetchTypeList" />
            <el-button type="primary" @click="openTypeDialog()">新增类型</el-button>
          </div>

          <el-table :data="typeList" v-loading="typeLoading" stripe :header-cell-style="{ background: 'var(--oa-surface-soft)', color: 'var(--oa-muted)' }">
            <el-table-column prop="dictName" label="字典名称" min-width="150" />
            <el-table-column prop="dictType" label="字典类型" min-width="150" />
            <el-table-column label="创建时间" prop="createTime" width="170" />
            <el-table-column label="操作" width="200" align="center">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="showDictData(row)">字典数据</el-button>
                <el-button type="primary" link size="small" @click="openTypeDialog(row)">编辑</el-button>
                <el-popconfirm title="确定删除该字典类型？删除后关联的字典数据也将清除。" @confirm="handleDeleteType(row.id)">
                  <template #reference>
                    <el-button type="danger" link size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无字典类型" />
            </template>
          </el-table>

          <div class="mt-4 flex justify-end">
            <OaPagination v-model:current-page="typePageNum" v-model:page-size="typePageSize" :total="typeTotal" :page-sizes="[10, 20, 50]" @change="fetchTypeList" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="字典数据" name="data">
          <div class="mb-4 flex justify-between">
            <div class="flex items-center gap-3">
              <el-input v-model="dataTypeFilter" placeholder="搜索字典数据/类型" style="width: 240px" clearable :prefix-icon="Search" @clear="fetchDataList" @keyup.enter="fetchDataList" />
              <el-select v-model="dataTypeSelect" placeholder="按字典类型筛选" clearable style="width: 200px" @change="handleDataTypeSelect">
                <el-option v-for="t in typeList" :key="t.dictType" :label="t.dictName" :value="t.dictType" />
              </el-select>
            </div>
            <el-button type="primary" @click="openDataDialog()">新增数据</el-button>
          </div>

          <el-table :data="dataList" v-loading="dataLoading" stripe :header-cell-style="{ background: 'var(--oa-surface-soft)', color: 'var(--oa-muted)' }">
            <el-table-column prop="dictLabel" label="字典标签" min-width="120" />
            <el-table-column prop="dictValue" label="字典值" min-width="100" />
            <el-table-column prop="dictType" label="字典类型" min-width="120" />
            <el-table-column prop="dictSort" label="排序" width="80" align="center" />
            <el-table-column label="操作" width="150" align="center">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openDataDialog(row)">编辑</el-button>
                <el-popconfirm title="确定删除?" @confirm="handleDeleteData(row.id)">
                  <template #reference>
                    <el-button type="danger" link size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无字典数据" />
            </template>
          </el-table>

          <div class="mt-4 flex justify-end">
            <OaPagination v-model:current-page="dataPageNum" v-model:page-size="dataPageSize" :total="dataTotal" :page-sizes="[10, 20, 50]" @change="fetchDataList" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 字典类型弹窗 -->
    <el-dialog v-model="typeDialogVisible" :title="typeForm.id ? '编辑字典类型' : '新增字典类型'" width="500px" :close-on-click-modal="false">
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="90px">
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="typeForm.dictName" placeholder="请输入字典名称" />
        </el-form-item>
        <el-form-item label="字典类型" prop="dictType">
          <el-input v-model="typeForm.dictType" placeholder="请输入字典类型" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="typeSaving" @click="handleSaveType">确定</el-button>
      </template>
    </el-dialog>

    <!-- 字典数据弹窗 -->
    <el-dialog v-model="dataDialogVisible" :title="dataForm.id ? '编辑字典数据' : '新增字典数据'" width="500px" :close-on-click-modal="false">
      <el-form ref="dataFormRef" :model="dataForm" :rules="dataRules" label-width="90px">
        <el-form-item label="字典类型" prop="dictType">
          <el-input v-model="dataForm.dictType" placeholder="请输入字典类型" />
        </el-form-item>
        <el-form-item label="字典标签" prop="dictLabel">
          <el-input v-model="dataForm.dictLabel" placeholder="请输入字典标签" />
        </el-form-item>
        <el-form-item label="字典值" prop="dictValue">
          <el-input v-model="dataForm.dictValue" placeholder="请输入字典值" />
        </el-form-item>
        <el-form-item label="排序" prop="dictSort">
          <el-input-number v-model="dataForm.dictSort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dataSaving" @click="handleSaveData">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import { Search } from "@element-plus/icons-vue";
import {
  getDictTypePage, addDictType, updateDictType, deleteDictType,
  getDictDataPage, addDictData, updateDictData, deleteDictData
} from "@/api/dict";

const activeTab = ref("type");

// --- 字典类型 ---
const typeLoading = ref(false);
const typeList = ref<any[]>([]);
const typePageNum = ref(1);
const typePageSize = ref(10);
const typeTotal = ref(0);
const typeName = ref("");

const fetchTypeList = async () => {
  typeLoading.value = true;
  try {
    const res: any = await getDictTypePage({ pageNum: typePageNum.value, pageSize: typePageSize.value, dictName: typeName.value || undefined });
    typeList.value = res.data?.list || [];
    typeTotal.value = res.data?.total || 0;
  } catch {
    ElMessage.error("获取字典类型失败");
  } finally {
    typeLoading.value = false;
  }
};

const handleTabChange = (tab: string | number) => {
  if (tab === "data") {
    fetchDataList();
  }
};

const typeDialogVisible = ref(false);
const typeSaving = ref(false);
const typeFormRef = ref<FormInstance>();
const typeForm = reactive({ id: undefined as number | undefined, dictName: "", dictType: "" });
const typeRules = reactive<FormRules>({
  dictName: [{ required: true, message: "请输入字典名称", trigger: "blur" }],
  dictType: [{ required: true, message: "请输入字典类型", trigger: "blur" }]
});

const openTypeDialog = (row?: any) => {
  if (row) {
    Object.assign(typeForm, { id: row.id, dictName: row.dictName, dictType: row.dictType });
  } else {
    Object.assign(typeForm, { id: undefined, dictName: "", dictType: "" });
  }
  typeDialogVisible.value = true;
};

const handleSaveType = async () => {
  if (!typeFormRef.value) return;
  await typeFormRef.value.validate();
  typeSaving.value = true;
  try {
    if (typeForm.id) {
      await updateDictType(typeForm);
    } else {
      await addDictType(typeForm);
    }
    ElMessage.success("保存成功");
    typeDialogVisible.value = false;
    fetchTypeList();
  } catch {
    ElMessage.error("保存失败");
  } finally {
    typeSaving.value = false;
  }
};

const handleDeleteType = async (id: number) => {
  try {
    await deleteDictType(id);
    ElMessage.success("删除成功");
    fetchTypeList();
  } catch {
    ElMessage.error("删除失败");
  }
};

const showDictData = (row: any) => {
  dataTypeFilter.value = row.dictType;
  dataTypeSelect.value = row.dictType;
  activeTab.value = "data";
  dataPageNum.value = 1;
  fetchDataList();
};

// --- 字典数据 ---
const dataLoading = ref(false);
const dataList = ref<any[]>([]);
const dataPageNum = ref(1);
const dataPageSize = ref(10);
const dataTotal = ref(0);
const dataTypeFilter = ref("");
const dataTypeSelect = ref("");

const handleDataTypeSelect = (val: string) => {
  dataTypeFilter.value = val || "";
  dataPageNum.value = 1;
  fetchDataList();
};

const fetchDataList = async () => {
  dataLoading.value = true;
  try {
    const res: any = await getDictDataPage({ pageNum: dataPageNum.value, pageSize: dataPageSize.value, dictType: dataTypeFilter.value || undefined });
    dataList.value = res.data?.list || [];
    dataTotal.value = res.data?.total || 0;
  } catch {
    ElMessage.error("获取字典数据失败");
  } finally {
    dataLoading.value = false;
  }
};

const dataDialogVisible = ref(false);
const dataSaving = ref(false);
const dataFormRef = ref<FormInstance>();
const dataForm = reactive({ id: undefined as number | undefined, dictType: "", dictLabel: "", dictValue: "", dictSort: 0 });
const dataRules = reactive<FormRules>({
  dictType: [{ required: true, message: "请输入字典类型", trigger: "blur" }],
  dictLabel: [{ required: true, message: "请输入字典标签", trigger: "blur" }],
  dictValue: [{ required: true, message: "请输入字典值", trigger: "blur" }]
});

const openDataDialog = (row?: any) => {
  if (row) {
    Object.assign(dataForm, { id: row.id, dictType: row.dictType, dictLabel: row.dictLabel, dictValue: row.dictValue, dictSort: row.dictSort || 0 });
  } else {
    Object.assign(dataForm, { id: undefined, dictType: dataTypeFilter.value, dictLabel: "", dictValue: "", dictSort: 0 });
  }
  dataDialogVisible.value = true;
};

const handleSaveData = async () => {
  if (!dataFormRef.value) return;
  await dataFormRef.value.validate();
  dataSaving.value = true;
  try {
    if (dataForm.id) {
      await updateDictData(dataForm);
    } else {
      await addDictData(dataForm);
    }
    ElMessage.success("保存成功");
    dataDialogVisible.value = false;
    fetchDataList();
  } catch {
    ElMessage.error("保存失败");
  } finally {
    dataSaving.value = false;
  }
};

const handleDeleteData = async (id: number) => {
  try {
    await deleteDictData(id);
    ElMessage.success("删除成功");
    fetchDataList();
  } catch {
    ElMessage.error("删除失败");
  }
};

onMounted(() => {
  fetchTypeList();
  fetchDataList();
});
</script>
