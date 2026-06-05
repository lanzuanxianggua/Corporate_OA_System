<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';

const router = useRouter();
const username = ref('admin');
const password = ref('admin123');
const loading = ref(false);

async function handleLogin() {
  loading.value = true;
  try {
    // 占位: 真实实现调用 authApi.login(username, password)
    // const res = await authApi.login({ username: username.value, password: password.value });
    // localStorage.setItem('oa_access_token', res.accessToken);
    ElMessage.success('登录成功 (占位)');
    router.push('/dashboard');
  } catch (e) {
    ElMessage.error('登录失败');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2 class="title">企业 OA 系统 v2</h2>
      <p class="subtitle">v2.0.0-SNAPSHOT</p>
      <el-form @submit.prevent="handleLogin">
        <el-form-item>
          <el-input v-model="username" placeholder="用户名" size="large">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-input v-model="password" type="password" placeholder="密码" size="large" show-password>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-button type="primary" size="large" class="w-full" :loading="loading" @click="handleLogin">
          登录
        </el-button>
      </el-form>
      <p class="hint">默认账号: admin / admin123</p>
    </el-card>
  </div>
</template>

<style lang="scss" scoped>
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: linear-gradient(135deg, #1890ff 0%, #722ed1 100%);
}
.login-card {
  width: 400px;
  padding: 32px;
  border-radius: 8px;
  .title {
    text-align: center;
    margin: 0 0 8px;
  }
  .subtitle {
    text-align: center;
    color: #999;
    margin-bottom: 24px;
  }
  .hint {
    margin-top: 16px;
    text-align: center;
    color: #999;
    font-size: 12px;
  }
}
.w-full {
  width: 100%;
}
</style>
