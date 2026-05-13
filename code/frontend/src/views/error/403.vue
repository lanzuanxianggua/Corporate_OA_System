<template>
  <div class="flex flex-col items-center justify-center h-full">
    <div class="text-9xl font-bold text-[#dcdfe6] mb-4">403</div>
    <p class="text-base text-[#606266] mb-8">抱歉，您没有权限访问该页面</p>
    <el-button type="primary" plain @click="$router.push('/welcome')">返回首页</el-button>
  </div>
</template>
