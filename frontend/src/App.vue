<template>
  <!-- 全局消息提示（axios 失败自动弹出，页面也可主动调用 toast*） -->
  <CxMessage ref="msgRef" />
  <!-- /admin 走全屏管理端，不套站点外壳 -->
  <router-view v-if="isAdmin" />
  <LayoutView v-else />
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import LayoutView from './layout/LayoutView.vue'
import CxMessage from './components/cx/CxMessage.vue'
import { registerToast } from './utils/toast'

const route = useRoute()
// 路由未就绪时用地址栏兜底，避免 /admin 首屏闪现站点外壳
const isAdmin = computed(() => {
  const path = route.matched.length ? route.path : window.location.pathname
  return path.startsWith('/admin')
})

const msgRef = ref(null)
onMounted(() => {
  registerToast(msgRef.value)
})
</script>
