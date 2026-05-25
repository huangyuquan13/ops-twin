<template>
  <div class="forbidden">
    <div class="code">403</div>
    <p class="desc">您没有权限访问此页面</p>
    <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
  </div>
</template>

<style scoped>
.forbidden {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  height: 100%; color: #909399;
}
.code { font-size: 72px; font-weight: 700; color: #c0c4cc; line-height: 1; }
.desc { font-size: 16px; margin: 16px 0 24px; }
</style>
