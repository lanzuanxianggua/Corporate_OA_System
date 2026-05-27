<template>
  <div>
    <el-card>
      <template #header><span class="font-medium">发送消息</span></template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="接收人" prop="receiverId">
          <el-select v-model="form.receiverId" placeholder="请选择接收人" filterable style="width: 100%">
            <el-option v-for="emp in employeeList" :key="emp.id" :label="`${emp.empName}（${emp.empCode}）`" :value="emp.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="消息标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入消息标题" />
        </el-form-item>
        <el-form-item label="消息内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入消息内容" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="sending" @click="handleSend">发送</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage } from "element-plus";
import { sendMessage } from "@/api/message";
import { getEmployeePage } from "@/api/employee";
import { useUserStore } from "@/store/user";
import type { FormInstance, FormRules } from "element-plus";

const userStore = useUserStore();
const formRef = ref<FormInstance>();
const sending = ref(false);
const employeeList = ref<any[]>([]);
const form = reactive({ receiverId: undefined as number | undefined, title: "", content: "" });

const rules: FormRules = {
  receiverId: [{ required: true, message: "请选择接收人", trigger: ["blur", "change"] }],
  title: [{ required: true, message: "请输入标题", trigger: "blur" }],
  content: [{ required: true, message: "请输入内容", trigger: "blur" }]
};

const fetchEmployees = async () => {
  try {
    const res: any = await getEmployeePage({ pageNum: 1, pageSize: 200 });
    if (res.data?.list) {
      const myId = userStore.userInfo?.empId;
      employeeList.value = res.data.list.filter((e: any) => e.id !== myId);
    }
  } catch {}
};

const handleSend = async () => {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
  } catch {
    return;
  }
  sending.value = true;
  try {
    await sendMessage({
      receiverId: form.receiverId,
      msgType: 0,
      title: form.title,
      content: form.content
    });
    ElMessage.success("发送成功");
    formRef.value.resetFields();
  } catch (e: any) {
    ElMessage.error(e.message || "发送失败");
  } finally {
    sending.value = false;
  }
};

onMounted(fetchEmployees);
</script>
