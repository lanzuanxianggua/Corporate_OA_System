<template>
  <div>
    <el-card class="max-w-2xl">
      <template #header><span class="font-medium">发送消息</span></template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="接收人ID" prop="receiverId">
          <el-input v-model.number="form.receiverId" placeholder="请输入接收者员工ID" />
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
import { ref, reactive } from "vue";
import { ElMessage } from "element-plus";
import { sendMessage } from "@/api/message";
import { useUserStore } from "@/store/user";
import type { FormInstance, FormRules } from "element-plus";

const userStore = useUserStore();
const formRef = ref<FormInstance>();
const sending = ref(false);
const form = reactive({ receiverId: "", title: "", content: "" });

const rules: FormRules = {
  receiverId: [{ required: true, message: "请输入接收人", trigger: "blur" }],
  title: [{ required: true, message: "请输入标题", trigger: "blur" }],
  content: [{ required: true, message: "请输入内容", trigger: "blur" }]
};

const handleSend = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    sending.value = true;
    try {
      await sendMessage({
        senderId: userStore.userInfo?.empId,
        receiverId: Number(form.receiverId),
        msgType: 0,
        title: form.title,
        content: form.content
      });
      ElMessage.success("发送成功");
      form.receiverId = "";
      form.title = "";
      form.content = "";
    } catch (e: any) { ElMessage.error(e.message || "发送失败"); }
    finally { sending.value = false; }
  });
};
</script>
