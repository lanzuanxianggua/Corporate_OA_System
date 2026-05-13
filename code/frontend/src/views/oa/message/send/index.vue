<template>
  <div class="message-send-container">
    <el-card>
      <template #header>
        <span>发送消息</span>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width: 600px">
        <el-form-item label="接收人" prop="receivers">
          <el-select v-model="form.receivers" multiple filterable placeholder="选择接收人" style="width: 100%">
            <el-option label="张三" value="zhangsan" />
            <el-option label="李四" value="lisi" />
            <el-option label="王五" value="wangwu" />
            <el-option label="赵六" value="zhaoliu" />
          </el-select>
        </el-form-item>
        <el-form-item label="消息标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入消息标题" />
        </el-form-item>
        <el-form-item label="消息内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入消息内容" />
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-select v-model="form.priority" placeholder="选择优先级" style="width: 100%">
            <el-option label="普通" value="普通" />
            <el-option label="重要" value="重要" />
            <el-option label="紧急" value="紧急" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="loading">发送</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from "vue";
import { ElMessage } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";

const formRef = ref<FormInstance>();
const loading = ref(false);

const form = reactive({
  receivers: [] as string[],
  title: "",
  content: "",
  priority: "普通"
});

const rules: FormRules = {
  receivers: [{ required: true, message: "请选择接收人", trigger: "change" }],
  title: [{ required: true, message: "请输入消息标题", trigger: "blur" }],
  content: [{ required: true, message: "请输入消息内容", trigger: "blur" }]
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  await formRef.value.validate((valid) => {
    if (valid) {
      loading.value = true;
      setTimeout(() => {
        loading.value = false;
        ElMessage.success("发送成功");
        handleReset();
      }, 1000);
    }
  });
};

const handleReset = () => {
  formRef.value?.resetFields();
};
</script>

<style scoped lang="scss">
.message-send-container {
}
</style>