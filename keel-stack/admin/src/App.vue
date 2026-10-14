<script setup lang="ts">
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import { antdTheme } from '@/theme/tokens'
</script>

<template>
  <!-- 全局套一层 ConfigProvider：主题 token 和中文化在这里统一注入，各页面不用重复设置 -->
  <a-config-provider :theme="antdTheme" :locale="zhCN">
    <router-view />
  </a-config-provider>
</template>
