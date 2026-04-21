<template>
  <div class="post-add-page nexus-page">
    <div class="app-shell">
      <van-form @submit="onSubmit">
        <van-cell-group inset>
          <van-field
            v-model="postData.title"
            name="title"
            label="标题"
            placeholder="请输入帖子标题"
            :rules="[{ required: true, message: '请填写标题' }]"
            maxlength="256"
            show-word-limit
          />
          <van-field
            v-model="postData.content"
            name="content"
            label="内容"
            type="textarea"
            placeholder="请输入帖子内容"
            :rules="[{ required: true, message: '请填写内容' }]"
            rows="10"
            maxlength="5000"
            show-word-limit
          />
        </van-cell-group>
        <div style="margin: 16px">
          <van-button round block type="primary" native-type="submit" :loading="submitting">
            发布
          </van-button>
        </div>
      </van-form>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import myAxios from '../request'

const route = useRoute()
const router = useRouter()

const teamId = ref<number>(Number(route.query.teamId))
const submitting = ref(false)

const postData = ref({
  title: '',
  content: ''
})

const onSubmit = async () => {
  if (submitting.value) return

  submitting.value = true
  try {
    const res = await myAxios.post('/post/add', {
      teamId: teamId.value,
      title: postData.value.title,
      content: postData.value.content
    })

    if (res?.data?.code === 0) {
      showToast('发布成功')
      // 返回小组详情页
      router.back()
    } else {
      showToast(res?.data?.message || '发布失败')
    }
  } catch (error) {
    console.error('发布失败:', error)
    showToast('发布失败，请重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.nexus-page {
  min-height: 100vh;
  background: transparent;
  color: var(--color-text-primary);
}

.app-shell {
  padding: 16px;
}
</style>
