<template>
  <div class="flex flex-col items-center justify-center h-full">
    <div class="text-9xl font-bold text-[#dcdfe6] mb-4">500</div>
    <p class="text-base text-[#606266] mb-8">抱歉，服务器出错了</p>
    <el-button type="primary" plain @click="$router.push('/welcome')">返回首页</el-button>
  </div>
</template>
