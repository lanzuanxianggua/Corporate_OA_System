<template>
  <el-dialog
    v-model="visible"
    title="新建请假申请"
    width="540px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" label-position="right">
      <el-form-item label="请假类型" prop="leaveType">
        <el-select v-model="form.leaveType" placeholder="请选择" style="width: 100%">
          <el-option v-for="opt in LEAVE_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>

      <el-form-item label="开始日期" prop="startDate">
        <el-date-picker
          v-model="form.startDate"
          type="date"
          placeholder="请选择开始日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item label="结束日期" prop="endDate">
        <el-date-picker
          v-model="form.endDate"
          type="date"
          placeholder="请选择结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          style="width: 100%"
          :disabled-date="disableEndDate"
        />
      </el-form-item>

      <el-form-item label="请假事由" prop="reason">
        <el-input
          v-model="form.reason"
          type="textarea"
          :rows="4"
          placeholder="请输入请假事由"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">提交</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import { hrLeaveApi, LEAVE_TYPE_OPTIONS } from "@/api/hr-leave";

const props = defineProps<{ visible: boolean }>();
const emit = defineEmits<{
  (e: "update:visible", v: boolean): void;
  (e: "success"): void;
}>();

const visible = ref(props.visible);
watch(
  () => props.visible,
  v => (visible.value = v)
);
watch(visible, v => emit("update:visible", v));

const formRef = ref<FormInstance>();
const submitting = ref(false);

const form = reactive<{ leaveType: string; startDate: string; endDate: string; reason: string }>({
  leaveType: "",
  startDate: "",
  endDate: "",
  reason: ""
});

const rules = reactive<FormRules>({
  leaveType: [{ required: true, message: "请选择请假类型", trigger: "change" }],
  startDate: [{ required: true, message: "请选择开始日期", trigger: "change" }],
  endDate: [
    { required: true, message: "请选择结束日期", trigger: "change" },
    {
      validator: (_rule, value, callback) => {
        if (!value) {
          callback();
          return;
        }
        if (form.startDate && value < form.startDate) {
          callback(new Error("结束日期不能早于开始日期"));
        } else {
          callback();
        }
      },
      trigger: "change"
    }
  ],
  reason: [{ max: 200, message: "事由最多 200 字", trigger: "blur" }]
});

const disableEndDate = (date: Date) => {
  if (!form.startDate) return false;
  const start = new Date(form.startDate);
  return date.getTime() < start.getTime();
};

const resetForm = () => {
  formRef.value?.resetFields();
  form.leaveType = "";
  form.startDate = "";
  form.endDate = "";
  form.reason = "";
};

const handleClose = () => {
  resetForm();
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
  } catch {
    return;
  }

  if (form.startDate && form.endDate && form.endDate < form.startDate) {
    ElMessage.warning("结束日期不能早于开始日期");
    return;
  }

  submitting.value = true;
  try {
    await hrLeaveApi.create({
      leaveType: form.leaveType,
      startDate: form.startDate,
      endDate: form.endDate,
      reason: form.reason || undefined
    });
    ElMessage.success("请假申请已提交");
    emit("success");
    resetForm();
  } catch {
    ElMessage.error("提交失败");
  } finally {
    submitting.value = false;
  }
};
</script>
