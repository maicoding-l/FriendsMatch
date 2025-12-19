<template>
  <div class="search-page">
    <!-- 可滚动区域：包含搜索、标签、树形选择 -->
    <div class="scroll-area">
      <div class="top-section">
        <form action="/">
          <van-search
            v-model="searchText"
            show-action
            placeholder="请输入搜索关键词"
            @search="onSearch"
            @cancel="onCancel"
          />
        </form>
        <van-divider content-position="left">已选标签</van-divider>
        <div class="selected-tags">
          <div v-if="activeIds.length === 0" class="empty">请选择标签</div>
          <div v-else class="tags">
            <van-tag
              v-for="tag in activeIds"
              :key="tag"
              closeable
              size="medium"
              type="primary"
              class="tag"
              @close="() => doClose(tag)"
            >{{ tag }}</van-tag>
          </div>
        </div>
      </div>

      <!-- 树形选择区：填满剩余空间 -->
      <div class="tree-section">
        <van-tree-select
          v-model:active-id="activeIds"
          v-model:main-active-index="activeIndex"
          :items="tagsList"
        />
      </div>
    </div>

    <!-- 提交按钮：绝对定位在 TabBar 正上方 -->
    <div class="submit-bar">
      <van-button type="primary" block @click="onConfirm">提交</van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const OriginalTagsList = [
  { text: '性别', children: [{ text: '男', id: '男' }, { text: '女', id: '女' }] },
  { text: '年级', children: [
    { text: '大一', id: '大一' }, { text: '大二', id: '大二' }, { text: '大三', id: '大三' },
    { text: '大四', id: '大四' }, { text: '研一', id: '研一' }, { text: '研二', id: '研二' }, { text: '研三', id: '研三' }
  ]},
  { text: '技术栈', children: [{ text: 'Java', id: 'Java' }, { text: 'Python', id: 'Python' }, { text: 'C++', id: 'C++' }] }
]
const activeIds = ref<string[]>([])
const activeIndex = ref(0)
const searchText = ref('')
const tagsList = ref(OriginalTagsList)

const doClose = (id: string) =>
  activeIds.value = activeIds.value.filter(t => t !== id)

const onCancel = () => {
  searchText.value = ''
  tagsList.value = OriginalTagsList
}

const onSearch = () => {
  tagsList.value = OriginalTagsList.map(p => ({
    ...p,
    children: p.children.filter(c => c.text.includes(searchText.value))
  }))
}

const onConfirm = () => {
  router.push({
    path: '/user/list',
    query: { tags: activeIds.value.join(',') }
  })
}
</script>

// ...existing code...

<style scoped>
.search-page {
  /* CSS 变量定义 */
  --tabbar-height: 50px; /* TabBar 高度，根据实际调整 */
  --btn-height: 48px;   /* 按钮高度 */
  --gap: 0px;           /* 按钮与 TabBar 间距，设为 0 让按钮紧贴 TabBar 上方 */

  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden; /* 防止页面整体滚动 */
}

/* 可滚动区域：占满页面高度 */
.scroll-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto; /* 内部滚动 */
  /* 去掉 padding-bottom，让树的内容区域自己预留按钮空间 */
}

.top-section {
  flex-shrink: 0; /* 顶部固定不压缩 */
}

.selected-tags {
  padding: 0 16px 12px 16px;
  min-height: 40px;
  display: flex;
  align-items: center;
}

.empty {
  color: #969799;
  font-size: 14px;
  width: 100%;
  text-align: center;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  width: 100%;
}

.tag {
  border-radius: 14px;
  padding: 2px 10px;
  font-size: 12px;
}

/* 树形选择区：占满 scroll-area 的剩余空间 */
.tree-section {
  flex: 1;
  min-height: 0; /* 关键：允许 flex 子项收缩 */
  display: flex;
}

.tree-section :deep(.van-tree-select) {
  flex: 1;
  display: flex;
  height: 100%;
}

.tree-section :deep(.van-tree-select__nav) {
  flex-shrink: 0;
}

.tree-section :deep(.van-tree-select__content) {
  flex: 1;
  overflow-y: auto;
  /* 在内容区域预留按钮空间，避免最后内容被遮 */
  padding-bottom: calc(var(--btn-height) + var(--gap));
}

/* 提交按钮：绝对定位在 TabBar 正上方 */
.submit-bar {
  position: absolute;
  left: 0;
  right: 0;
  bottom: calc(var(--tabbar-height) + var(--gap) + env(safe-area-inset-bottom)); /* 紧贴 TabBar 上方 */
  display: flex;
  justify-content: center;
  pointer-events: none; /* 透明区域不阻挡 TabBar 点击 */
}

.submit-bar .van-button {
  pointer-events: auto; /* 按钮本身可点击 */
  width: 82%;
  max-width: 420px;
  height: var(--btn-height);
  line-height: var(--btn-height);
  border-radius: 999px;
  font-size: 16px;
  box-shadow: 0 4px 12px -2px rgba(0,0,0,.15);
}

@media (max-width: 360px) {
  .submit-bar .van-button {
    width: 90%;
    font-size: 15px;
  }
}
</style>
