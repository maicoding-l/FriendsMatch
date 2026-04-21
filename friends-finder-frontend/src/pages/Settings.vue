<template>
  <div class="settings-page">
    <div class="settings-content">
      <!-- 外观设置 -->
      <div class="settings-section">
        <div class="section-header">
          <i class="ri-palette-line"></i>
          <span>外观</span>
        </div>
        <div class="glass-card">
          <van-cell title="颜色模式" center>
            <template #icon>
              <i :class="isDarkMode ? 'ri-moon-fill' : 'ri-sun-fill'" class="theme-icon"></i>
            </template>
            <template #right-icon>
              <van-switch
                v-model="isDarkMode"
                size="22px"
                @change="handleThemeChange"
                :active-color="isDarkMode ? '#a29bfe' : 'oklch(58% 0.15 40)'"
                :inactive-color="'rgba(128, 128, 128, 0.3)'"
              />
            </template>
          </van-cell>
          <div class="theme-preview">
            <div class="preview-label">
              {{ isDarkMode ? '暗夜模式' : '浅色模式' }}
            </div>
            <div class="preview-desc">
              {{ isDarkMode ? '深色背景，适合夜间使用' : '明亮背景，适合日间使用' }}
            </div>
          </div>
        </div>
      </div>

      <!-- 更多设置可以在这里添加 -->
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import { useTheme } from '../composables/useTheme';

// 主题管理
const { currentTheme, setTheme } = useTheme();

const isDarkMode = computed({
  get: () => currentTheme.value === 'dark',
  set: (value: boolean) => {
    setTheme(value ? 'dark' : 'light');
  }
});

const handleThemeChange = (value: boolean) => {
  setTheme(value ? 'dark' : 'light');
};
</script>

<style scoped>
@import url('https://cdn.jsdelivr.net/npm/remixicon@3.5.0/fonts/remixicon.css');

.settings-page {
  min-height: 100vh;
  background: var(--color-surface-base);
  padding-bottom: var(--space-24);
}

.settings-content {
  padding: var(--space-4);
}

/* Section */
.settings-section {
  margin-bottom: var(--space-6);
}

.section-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-2);
  margin-bottom: var(--space-3);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: var(--letter-spacing-wide);
}

.section-header i {
  font-size: 18px;
}

/* Glass Card */
.glass-card {
  background: var(--color-surface-raised);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: all var(--duration-base) var(--ease-out);
}

/* Theme Icon */
.theme-icon {
  font-size: 22px;
  margin-right: var(--space-3);
  color: var(--color-primary-500);
  transition: all var(--duration-base) var(--ease-out);
}

[data-theme="dark"] .theme-icon {
  color: #a29bfe;
}

/* Theme Preview */
.theme-preview {
  padding: var(--space-4) var(--space-5);
  border-top: 1px solid var(--color-border-subtle);
  background: var(--color-surface-overlay);
}

.preview-label {
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-1);
}

.preview-desc {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  line-height: var(--line-height-relaxed);
}

/* Van Cell Customization */
:deep(.van-cell) {
  background: transparent;
  color: var(--color-text-primary);
  padding: var(--space-4) var(--space-5);
}

:deep(.van-cell__title) {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
}

/* Van Switch Customization */
:deep(.van-switch) {
  transition: all var(--duration-base) var(--ease-out);
}

:deep(.van-switch__node) {
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

/* Light theme specific styles */
[data-theme="light"] .glass-card {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

[data-theme="light"] .theme-preview {
  background: var(--color-surface-base);
}

/* Dark theme specific styles */
[data-theme="dark"] .glass-card {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}
</style>
