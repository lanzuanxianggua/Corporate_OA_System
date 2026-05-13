<template>
  <div>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="基本信息" name="info">
        <div class="flex items-center gap-4 mb-6 p-6 bg-white rounded-lg" style="box-shadow:0 2px 12px rgba(0,0,0,.06)">
          <el-avatar :size="64" class="bg-[#409EFF]">
            {{ mineData?.empName?.charAt(0) || "U" }}
          </el-avatar>
          <div>
            <h3 class="text-lg font-medium text-[#303133]">{{ mineData?.empName }}</h3>
            <div class="mt-1">
              <el-tag v-for="role in mineData?.roles" :key="role" size="small" class="mr-1">{{ role }}</el-tag>
            </div>
          </div>
        </div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="用户名">{{ mineData?.username }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ mineData?.empName }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ mineData?.email || "-" }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ mineData?.phone || "-" }}</el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>
      <el-tab-pane label="修改密码" name="password">
        <el-card class="max-w-md">
          <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="100px">
            <el-form-item label="当前密码" prop="oldPwd">
              <el-input v-model="pwdForm.oldPwd" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPwd">
              <el-input v-model="pwdForm.newPwd" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPwd">
              <el-input v-model="pwdForm.confirmPwd" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="pwdLoading" @click="handleChangePwd">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage } from "element-plus";
import { getMine } from "@/api/system";
import { updatePassword } from "@/api/employee";
import { useUserStore } from "@/store/user";
import type { FormInstance, FormRules } from "element-plus";

const userStore = useUserStore();
const activeTab = ref("info");
const mineData = ref<any>(null);
const pwdFormRef = ref<FormInstance>();
const pwdLoading = ref(false);

const pwdForm = reactive({ oldPwd: "", newPwd: "", confirmPwd: "" });

const pwdRules: FormRules = {
  oldPwd: [{ required: true, message: "请输入当前密码", trigger: "blur" }],
  newPwd: [
    { required: true, message: "请输入新密码", trigger: "blur" },
    { min: 6, message: "密码至少6位", trigger: "blur" }
  ],
  confirmPwd: [
    { required: true, message: "请确认密码", trigger: "blur" },
    {
      validator: (_rule: any, value: string, callback: any) => {
        if (value !== pwdForm.newPwd) callback(new Error("两次密码不一致"));
        else callback();
      },
      trigger: "blur"
    }
  ]
};

const handleChangePwd = async () => {
  if (!pwdFormRef.value) return;
  await pwdFormRef.value.validate(async (valid) => {
    if (!valid) return;
    pwdLoading.value = true;
    try {
      const empId = userStore.userInfo?.empId || userStore.userInfo?.id;
      await updatePassword(empId, pwdForm.oldPwd, pwdForm.newPwd);
      ElMessage.success("密码修改成功");
      pwdForm.oldPwd = "";
      pwdForm.newPwd = "";
      pwdForm.confirmPwd = "";
    } catch (error: any) {
      ElMessage.error(error.message || "修改失败");
    } finally {
      pwdLoading.value = false;
    }
  });
};

onMounted(async () => {
  try {
    const res: any = await getMine();
    if (res.data) mineData.value = res.data;
  } catch {
    // ignore
  }
});
</script>
