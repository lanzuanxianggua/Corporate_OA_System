<template>
  <div class="flex flex-col items-center justify-center h-full">
    <div class="text-9xl font-bold text-[#dcdfe6] mb-4">404</div>
    <p class="text-base text-[#606266] mb-8">抱歉，您访问的页面不存在</p>
    <el-button type="primary" plain @click="$router.push('/welcome')">返回首页</el-button>
  </div>
</template>
