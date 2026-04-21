<template>
  <div>
    <BasicLayout />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import BasicLayout from './layouts/BasicLayout.vue'
import { useTheme } from './composables/useTheme'

const { initTheme } = useTheme()

onMounted(() => {
  // 初始化主题
  initTheme()
})
</script>

<style>
/* Import unified design system */
@import url('./styles/base.css');
@import url('./styles/components.css');

/* Root background - using design tokens */
body {
  background: radial-gradient(
    ellipse at top,
    var(--color-neutral-900) 0%,
    var(--color-surface-base) 40%
  );
  background-attachment: fixed;
}
</style>
