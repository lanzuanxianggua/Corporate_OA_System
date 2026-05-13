<template>
  <div class="account-settings-container">
    <el-card>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本信息" name="info">
          <div class="info-section">
            <div class="avatar-section">
              <el-avatar :size="80" style="background-color: #409EFF">张</el-avatar>
              <div class="user-info">
                <h3>张三</h3>
                <el-tag size="small">管理员</el-tag>
              </div>
            </div>
            <el-descriptions :column="2" border style="margin-top: 20px">
              <el-descriptions-item label="用户名">admin</el-descriptions-item>
              <el-descriptions-item label="昵称">张三</el-descriptions-item>
              <el-descriptions-item label="邮箱">zhangsan@oa.com</el-descriptions-item>
              <el-descriptions-item label="手机号">13800138000</el-descriptions-item>
              <el-descriptions-item label="部门">技术部</el-descriptions-item>
              <el-descriptions-item label="角色">管理员</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-tab-pane>
        <el-tab-pane label="修改密码" name="password">
          <el-form ref="formRef" :model="passwordForm" :rules="rules" label-width="100px" style="max-width: 400px">
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入当前密码" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请确认新密码" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSubmit">修改密码</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from "vue";
import { ElMessage } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";

const activeTab = ref("info");
const formRef = ref<FormInstance>();

const passwordForm = reactive({
  oldPassword: "",
  newPassword: "",
  confirmPassword: ""
});

const validateConfirmPassword = (rule: any, value: string, callback: any) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error("两次输入的密码不一致"));
  } else {
    callback();
  }
};

const rules: FormRules = {
  oldPassword: [{ required: true, message: "请输入当前密码", trigger: "blur" }],
  newPassword: [
    { required: true, message: "请输入新密码", trigger: "blur" },
    { min: 6, message: "密码长度不能少于6位", trigger: "blur" }
  ],
  confirmPassword: [
    { required: true, message: "请确认新密码", trigger: "blur" },
    { validator: validateConfirmPassword, trigger: "blur" }
  ]
};

const handleSubmit = () => {
  formRef.value?.validate((valid) => {
    if (valid) {
      ElMessage.success("密码修改成功");
      handleReset();
    }
  });
};

const handleReset = () => {
  formRef.value?.resetFields();
};
</script>

<style scoped lang="scss">
.account-settings-container {
  .info-section {
    max-width: 600px;
  }

  .avatar-section {
    display: flex;
    align-items: center;
    gap: 20px;

    .user-info {
      h3 {
        margin-bottom: 8px;
      }
    }
  }
}
</style>