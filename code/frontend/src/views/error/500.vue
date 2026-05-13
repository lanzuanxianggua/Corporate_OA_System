<template>
  <div class="error-page">
    <div class="error-code">500</div>
    <div class="error-message">抱歉，服务器发生错误</div>
    <el-button type="primary" @click="goHome">返回首页</el-button>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from "vue-router";

const router = useRouter();

const goHome = () => {
  router.push("/welcome");
};
</script>

<style scoped lang="scss">
.error-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background-color: #f5f7fa;

  .error-code {
    font-size: 120px;
    font-weight: bold;
    color: #c0c4cc;
    margin-bottom: 20px;
  }

  .error-message {
    font-size: 18px;
    color: #606266;
    margin-bottom: 30px;
  }
}
</style>