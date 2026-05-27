<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <span class="text-base font-semibold text-[#303133]">字典管理</span>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="字典类型" name="type">
          <div class="mb-4 flex justify-between">
            <el-input v-model="typeName" placeholder="搜索字典名称" style="width: 240px" clearable @clear="fetchTypeList" @keyup.enter="fetchTypeList">
              <template #append>
                <el-button @click="fetchTypeList">
                  <el-icon><Search /></el-icon>
                </el-button>
              </template>
            </el-input>
            <el-button type="primary" @click="openTypeDialog()">新增类型</el-button>
          </div>

          <el-table :data="typeList" v-loading="typeLoading" stripe :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
            <el-table-column prop="dictName" label="字典名称" min-width="120" />
            <el-table-column prop="dictType" label="字典类型" min-width="120" />
            <el-table-column label="操作" width="180" align="center">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="showDictData(row)">字典数据</el-button>
                <el-button type="primary" link size="small" @click="openTypeDialog(row)">编辑</el-button>
                <el-popconfirm title="确定删除?" @confirm="handleDeleteType(row.id)">
                  <template #reference>
                    <el-button type="danger" link size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>

          <div class="mt-4 flex justify-end">
            <el-pagination v-model:current-page="typePageNum" v-model:page-size="typePageSize" :total="typeTotal" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" background @change="fetchTypeList" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="字典数据" name="data">
          <div class="mb-4 flex justify-between">
            <el-input v-model="dataTypeFilter" placeholder="搜索字典数据" style="width: 240px" clearable @clear="fetchDataList" @keyup.enter="fetchDataList">
              <template #append>
                <el-button @click="fetchDataList">
                  <el-icon><Search /></el-icon>
                </el-button>
              </template>
            </el-input>
            <el-button type="primary" @click="openDataDialog()">新增数据</el-button>
          </div>

          <el-table :data="dataList" v-loading="dataLoading" stripe :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
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
          </el-table>

          <div class="mt-4 flex justify-end">
            <el-pagination v-model:current-page="dataPageNum" v-model:page-size="dataPageSize" :total="dataTotal" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" background @change="fetchDataList" />
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
  } finally {
    typeLoading.value = false;
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
  } finally {
    typeSaving.value = false;
  }
};

const handleDeleteType = async (id: number) => {
  await deleteDictType(id);
  ElMessage.success("删除成功");
  fetchTypeList();
};

const showDictData = (row: any) => {
  dataTypeFilter.value = row.dictType;
  activeTab.value = "data";
  fetchDataList();
};

// --- 字典数据 ---
const dataLoading = ref(false);
const dataList = ref<any[]>([]);
const dataPageNum = ref(1);
const dataPageSize = ref(10);
const dataTotal = ref(0);
const dataTypeFilter = ref("");

const fetchDataList = async () => {
  dataLoading.value = true;
  try {
    const res: any = await getDictDataPage({ pageNum: dataPageNum.value, pageSize: dataPageSize.value, dictType: dataTypeFilter.value || undefined });
    dataList.value = res.data?.list || [];
    dataTotal.value = res.data?.total || 0;
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
  } finally {
    dataSaving.value = false;
  }
};

const handleDeleteData = async (id: number) => {
  await deleteDictData(id);
  ElMessage.success("删除成功");
  fetchDataList();
};

onMounted(() => {
  fetchTypeList();
  fetchDataList();
});
</script>
