<script setup lang="ts">
import { ref, reactive, computed } from "vue";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import { sendSystemMessage } from "@/api/oa/message";

defineOptions({ name: "OaMessageSend" });

/** 部门选项 */
interface Department {
  id: number;
  name: string;
}

const formRef = ref<FormInstance>();
const submitting = ref(false);

const departments = ref<Department[]>([
  { id: 1, name: "技术部" },
  { id: 2, name: "市场部" },
  { id: 3, name: "人事部" },
  { id: 4, name: "财务部" }
]);

const targetTypeOptions = [
  { label: "全员", value: "all" },
  { label: "指定部门", value: "department" }
];

const form = reactive({
  title: "",
  content: "",
  targetType: "all",
  targetId: undefined as number | undefined
});

const rules = reactive<FormRules>({
  title: [{ required: true, message: "请输入消息标题", trigger: "blur" }],
  content: [{ required: true, message: "请输入消息内容", trigger: "blur" }],
  targetType: [{ required: true, message: "请选择发送目标类型", trigger: "change" }],
  targetId: [
    {
      required: true,
      validator: (_rule, value, callback) => {
        if (form.targetType === "department" && !value) {
          callback(new Error("请选择目标部门"));
        } else {
          callback();
        }
      },
      trigger: "change"
    }
  ]
});

/** 是否显示部门选择 */
const showDepartmentSelect = computed(() => form.targetType === "department");

/** 目标类型变化时清空部门 */
function handleTargetTypeChange() {
  form.targetId = undefined;
}

/** 提交发送 */
async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  submitting.value = true;
  try {
    const params: any = {
      title: form.title,
      content: form.content,
      targetType: form.targetType
    };
    if (form.targetType === "department" && form.targetId) {
      params.targetId = form.targetId;
    }
    await sendSystemMessage(params);
    ElMessage.success("消息发送成功");
    resetForm();
  } catch {
    ElMessage.error("消息发送失败");
  } finally {
    submitting.value = false;
  }
}

/** 重置表单 */
function resetForm() {
  formRef.value?.resetFields();
}
</script>

<template>
  <div class="oa-message-send">
    <el-card shadow="hover">
      <template #header>
        <span class="card-title">发送系统消息</span>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        style="max-width: 700px"
      >
        <el-form-item label="消息标题" prop="title">
          <el-input
            v-model="form.title"
            placeholder="请输入消息标题"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="发送目标" prop="targetType">
          <el-select
            v-model="form.targetType"
            placeholder="请选择发送目标"
            style="width: 100%"
            @change="handleTargetTypeChange"
          >
            <el-option
              v-for="opt in targetTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item v-if="showDepartmentSelect" label="目标部门" prop="targetId">
          <el-select
            v-model="form.targetId"
            placeholder="请选择目标部门"
            style="width: 100%"
          >
            <el-option
              v-for="dept in departments"
              :key="dept.id"
              :label="dept.name"
              :value="dept.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="消息内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="8"
            placeholder="请输入消息内容"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            发送消息
          </el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.oa-message-send {
  padding: 20px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}
</style>
