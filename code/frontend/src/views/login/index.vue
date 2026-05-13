<template>
  <div class="login-container">
    <div class="login-left">
      <div class="decoration">
        <div class="circle circle-1"></div>
        <div class="circle circle-2"></div>
        <div class="circle circle-3"></div>
        <div class="rectangle"></div>
      </div>
    </div>
    <div class="login-right">
      <div class="login-card">
        <div class="login-header">
          <el-icon size="48" color="#409EFF"><OfficeBuilding /></el-icon>
          <h1>OA办公系统</h1>
        </div>
        <el-form ref="loginFormRef" :model="loginForm" :rules="rules" class="login-form">
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
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-form-item>
            <div class="remember-me">
              <el-checkbox v-model="loginForm.remember">记住我</el-checkbox>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" :loading="loading" class="login-btn" @click="handleLogin">
              登 录
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { login } from "@/api/auth";
import type { FormInstance, FormRules } from "element-plus";

const router = useRouter();
const loginFormRef = ref<FormInstance>();
const loading = ref(false);

const loginForm = reactive({
  username: "",
  password: "",
  remember: false
});

const rules: FormRules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }]
};

const handleLogin = async () => {
  if (!loginFormRef.value) return;

  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return;

    loading.value = true;
    try {
      const res: any = await login({
        username: loginForm.username,
        password: loginForm.password
      });

      if (res.code === 0 || res.code === 200) {
        localStorage.setItem("token", res.data.token);
        localStorage.setItem("userInfo", JSON.stringify(res.data));
        ElMessage.success("登录成功");
        router.push("/welcome");
      } else {
        ElMessage.error(res.message || "登录失败");
      }
    } catch (error: any) {
      ElMessage.error(error.message || "登录失败，请检查网络");
    } finally {
      loading.value = false;
    }
  });
};
</script>

<style scoped lang="scss">
.login-container {
  display: flex;
  width: 100%;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-left {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #409EFF 0%, #667eea 100%);
  position: relative;
  overflow: hidden;
}

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

.circle-1 {
  width: 200px;
  height: 200px;
  top: 0;
  left: 0;
}

.circle-2 {
  width: 150px;
  height: 150px;
  bottom: 50px;
  right: 50px;
  background: rgba(255, 255, 255, 0.15);
}

.circle-3 {
  width: 100px;
  height: 100px;
  bottom: 0;
  left: 100px;
  background: rgba(255, 255, 255, 0.1);
}

.rectangle {
  position: absolute;
  width: 180px;
  height: 180px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%) rotate(45deg);
  background: rgba(255, 255, 255, 0.08);
}

.login-right {
  width: 500px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f7fa;
}

.login-card {
  width: 380px;
  padding: 40px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.login-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 32px;

  h1 {
    margin-top: 16px;
    font-size: 24px;
    font-weight: bold;
    color: #303133;
  }
}

.login-form {
  width: 100%;
}

.remember-me {
  width: 100%;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
}
</style>