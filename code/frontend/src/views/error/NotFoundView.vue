<script setup lang="ts">
import { useRouter } from 'vue-router';
import { ElButton } from 'element-plus';

const router = useRouter();
</script>

<template>
  <div class="not-found">
    <h1>404</h1>
    <p>页面不存在</p>
    <ElButton type="primary" @click="router.push('/')">返回首页</ElButton>
  </div>
</template>

<style lang="scss" scoped>
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  h1 {
    font-size: 96px;
    color: #1890ff;
    margin: 0;
  }
  p {
    color: #999;
    margin: 16px 0;
  }
}
</style>
