<template>
  <div class="flex w-full h-screen">
    <div
      class="flex-1 flex items-center justify-center relative overflow-hidden"
      style="background: linear-gradient(135deg, #409eff 0%, #667eea 100%)"
    >
      <div class="decoration">
        <div class="circle c1"></div>
        <div class="circle c2"></div>
        <div class="circle c3"></div>
        <div class="rect"></div>
      </div>
      <div class="text-white text-center z-10">
        <h1 class="text-4xl font-bold mb-4">OA办公系统</h1>
        <p class="text-lg opacity-80">高效协同 · 智慧办公</p>
      </div>
    </div>
    <div
      class="w-[500px] flex items-center justify-center"
      style="background-color: #f5f7fa"
    >
      <div
        class="w-[380px] p-10 bg-white rounded-xl"
        style="box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1)"
      >
        <div class="flex flex-col items-center mb-8">
          <el-icon :size="48" color="#409EFF"><OfficeBuilding /></el-icon>
          <h1 class="mt-4 text-2xl font-bold text-[#303133]">OA办公系统</h1>
        </div>
        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="rules"
          class="w-full"
        >
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              prefix-icon="User"
              size="large"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              size="large"
              show-password
            />
          </el-form-item>
          <el-form-item prop="captchaCode">
            <div class="flex w-full gap-2">
              <el-input
                v-model="loginForm.captchaCode"
                placeholder="请输入验证码"
                prefix-icon="Key"
                size="large"
                @keyup.enter="handleLogin"
              />
              <div
                class="h-[40px] min-w-[120px] rounded cursor-pointer flex items-center justify-center border border-gray-200 overflow-hidden"
                @click="refreshCaptcha"
              >
                <img
                  v-if="captchaImg"
                  :src="captchaImg"
                  alt="验证码"
                  class="h-full w-full object-cover"
                />
                <span v-else class="text-xs text-gray-400">点击获取</span>
              </div>
            </div>
          </el-form-item>
          <el-form-item>
            <el-checkbox v-model="loginForm.remember">记住我</el-checkbox>
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              class="w-full h-11 text-base"
              @click="handleLogin"
            >
              登 录
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { useUserStore } from "@/store/user";
import { getCaptcha } from "@/api/auth";
import type { FormInstance, FormRules } from "element-plus";

const router = useRouter();
const userStore = useUserStore();
const loginFormRef = ref<FormInstance>();
const loading = ref(false);
const captchaImg = ref("");
const captchaUuid = ref("");

const loginForm = reactive({
  username: "",
  password: "",
  captchaCode: "",
  remember: false
});

const rules: FormRules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }],
  captchaCode: [{ required: true, message: "请输入验证码", trigger: "blur" }]
};

const refreshCaptcha = async () => {
  try {
    const res = await getCaptcha();
    if (res.data) {
      captchaImg.value = res.data.img;
      captchaUuid.value = res.data.uuid;
      loginForm.captchaCode = "";
    }
  } catch {
    // ignore
  }
};

onMounted(() => {
  refreshCaptcha();
  const saved = localStorage.getItem("remembered_username");
  if (saved) {
    loginForm.username = saved;
    loginForm.remember = true;
  }
});

const handleLogin = async () => {
  if (!loginFormRef.value) return;
  try {
    await loginFormRef.value.validate();
  } catch {
    return;
  }
  loading.value = true;
  try {
    await userStore.loginAction(loginForm.username, loginForm.password, captchaUuid.value, loginForm.captchaCode);
    if (loginForm.remember) {
      localStorage.setItem("remembered_username", loginForm.username);
    } else {
      localStorage.removeItem("remembered_username");
    }
    ElMessage.success("登录成功");
    router.push("/welcome");
  } catch (error: any) {
    ElMessage.error(error.message || "登录失败");
    refreshCaptcha();
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.decoration {
  position: relative;
  width: 400px;
  height: 400px;
}
.circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}
.c1 {
  width: 200px;
  height: 200px;
  top: 0;
  left: 0;
}
.c2 {
  width: 150px;
  height: 150px;
  bottom: 50px;
  right: 50px;
  background: rgba(255, 255, 255, 0.15);
}
.c3 {
  width: 100px;
  height: 100px;
  bottom: 0;
  left: 100px;
  background: rgba(255, 255, 255, 0.1);
}
.rect {
  position: absolute;
  width: 180px;
  height: 180px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%) rotate(45deg);
  background: rgba(255, 255, 255, 0.08);
}
</style>
