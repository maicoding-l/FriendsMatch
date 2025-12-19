<template>
  <div class="nexus-page">
    <canvas ref="bgCanvas" class="bg-canvas"></canvas>
    <div class="app-shell">
      <section class="panel">
        <div class="page-title">
          <p class="eyebrow">Nexus</p>
          <div class="title-row">
            <span>图谱联结</span>
            <span class="dot">•</span>
            <span class="subtitle">mock 数据演示互动</span>
          </div>
        </div>

        <div class="glass-card graph-card">
          <div ref="nexusRef" class="graph-placeholder"></div>
          <div class="graph-hint">点击右侧列表聚焦关系</div>
        </div>

        <div class="section-header compact">
          <div>
            <p class="section-kicker">灵魂共鸣 · 推荐好友</p>
            <p class="section-sub">基于共同喜欢的物品（mock）</p>
          </div>
        </div>

        <div
          v-for="user in recommendedUsers"
          :key="user.id"
          class="match-card"
          :class="{ active: selectedUserId === user.id }"
          @click="focusUser(user)"
        >
          <div class="avatar-wrap">
            <img :src="user.avatar" alt="" />
            <span class="badge">{{ user.match }}%</span>
          </div>
          <div class="match-body">
            <div class="match-name">{{ user.name }}</div>
            <div class="match-reason">
              <i class="ri-link"></i>
              共同喜欢
              <span class="accent">{{ user.reason }}</span>
            </div>
          </div>
          <van-icon name="arrow" color="#8e9aaf" />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { showToast } from 'vant'

const bgCanvas = ref<HTMLCanvasElement | null>(null)
const nexusRef = ref<HTMLDivElement | null>(null)
const chartInstance = ref<any>(null)
let resizeGraphHandler: (() => void) | null = null
let bgAnimation: number | null = null
let bgResizeHandler: (() => void) | null = null

const selectedUserId = ref<string | null>(null)

const recommendedUsers = ref([
  {
    id: 'u1',
    name: 'Alice',
    match: 98,
    reason: '《三体》',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Alice',
  },
  {
    id: 'u2',
    name: 'Neo',
    match: 92,
    reason: '《黑客帝国》',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Neo',
  },
  {
    id: 'u3',
    name: 'Carol',
    match: 88,
    reason: 'Pink Floyd',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Carol',
  },
])

const mockItems = [
  { id: 1, title: '三体全套', itemType: 1, creator: '刘慈欣', popularity: 96 },
  { id: 2, title: '奥本海默', itemType: 2, creator: 'Christopher Nolan', popularity: 92 },
  { id: 3, title: 'Dark Side of the Moon', itemType: 3, creator: 'Pink Floyd', popularity: 91 },
  { id: 4, title: '赛博朋克：边缘行者', itemType: 2, popularity: 89 },
]

const loadEcharts = () =>
  new Promise<any>((resolve, reject) => {
    if ((window as any).echarts) {
      resolve((window as any).echarts)
      return
    }
    const script = document.createElement('script')
    script.src = 'https://cdn.jsdelivr.net/npm/echarts@5.5.1/dist/echarts.min.js'
    script.onload = () => resolve((window as any).echarts)
    script.onerror = reject
    document.head.appendChild(script)
  })

const renderGraph = async () => {
  const el = nexusRef.value
  if (!el) return
  try {
    const echarts = await loadEcharts()
    if (chartInstance.value) {
      chartInstance.value.dispose()
    }
    if (resizeGraphHandler) {
      window.removeEventListener('resize', resizeGraphHandler)
    }
    chartInstance.value = echarts.init(el)

    const itemNodes = mockItems.map((item) => ({
      id: `item-${item.id}`,
      name: item.title,
      symbolSize: 26,
      category: 1,
      itemType: item.itemType,
    }))

    const userNodes = recommendedUsers.value.map((user) => ({
      id: `user-${user.id}`,
      name: user.name,
      symbolSize: 34,
      category: 2,
    }))

    const links = [
      ...itemNodes.map((node) => ({ source: 'me', target: node.id })),
      ...recommendedUsers.value.map((user, index) => ({
        source: `user-${user.id}`,
        target: itemNodes[index % itemNodes.length]?.id,
      })),
    ]

    const isNodeHighlighted = (nodeId: string) => {
      if (!selectedUserId.value) return true
      if (nodeId === 'me' || nodeId === `user-${selectedUserId.value}`) return true
      const link = links.find((l) => l.source === `user-${selectedUserId.value}`)
      return link?.target === nodeId
    }

    const isLinkHighlighted = (link: { source: string; target: string }) => {
      if (!selectedUserId.value) return true
      return (
        link.source === `user-${selectedUserId.value}` ||
        link.target === `user-${selectedUserId.value}` ||
        (link.source === 'me' &&
          links.find((l) => l.source === `user-${selectedUserId.value}`)?.target === link.target)
      )
    }

    const option = {
      color: ['#a29bfe', '#74b9ff', '#fab1a0'],
      tooltip: {},
      series: [
        {
          type: 'graph',
          layout: 'force',
          data: [
            {
              id: 'me',
              name: 'Me',
              symbolSize: 44,
              category: 0,
              itemStyle: { borderColor: '#fff', borderWidth: 2 },
            },
            ...itemNodes.map((node) => ({
              ...node,
              itemStyle: { opacity: isNodeHighlighted(node.id) ? 1 : 0.25 },
            })),
            ...userNodes.map((node) => ({
              ...node,
              itemStyle: { opacity: isNodeHighlighted(node.id) ? 1 : 0.25 },
            })),
          ],
          links: links.map((link) => ({
            ...link,
            lineStyle: {
              opacity: isLinkHighlighted(link) ? 0.8 : 0.15,
              width: isLinkHighlighted(link) ? 2 : 1,
              color: '#fff',
            },
          })),
          categories: [{ name: '我' }, { name: '兴趣物品' }, { name: '推荐好友' }],
          roam: true,
          label: { show: true, color: '#fff' },
          force: { repulsion: 230, edgeLength: 110 },
        },
      ],
    }

    chartInstance.value.setOption(option)
    resizeGraphHandler = () => chartInstance.value?.resize()
    window.addEventListener('resize', resizeGraphHandler)
  } catch (error) {
    console.error('render graph failed', error)
    showToast('图谱加载失败')
  }
}

const focusUser = (user: (typeof recommendedUsers.value)[number]) => {
  selectedUserId.value = user.id
  renderGraph()
}

const initBackground = () => {
  const canvas = bgCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const particles = Array.from({ length: 70 }).map(() => ({
    x: Math.random() * window.innerWidth,
    y: Math.random() * window.innerHeight,
    r: Math.random() * 2 + 0.5,
    vx: (Math.random() - 0.5) * 0.2,
    vy: (Math.random() - 0.5) * 0.2,
  }))

  const resize = () => {
    canvas.width = window.innerWidth
    canvas.height = window.innerHeight
  }
  resize()
  bgResizeHandler = resize
  window.addEventListener('resize', resize)

  const draw = () => {
    const gradient = ctx.createLinearGradient(0, 0, canvas.width, canvas.height)
    gradient.addColorStop(0, '#0b0b1a')
    gradient.addColorStop(1, '#0f1b2f')
    ctx.fillStyle = gradient
    ctx.fillRect(0, 0, canvas.width, canvas.height)

    particles.forEach((p) => {
      p.x += p.vx
      p.y += p.vy
      if (p.x < 0 || p.x > canvas.width) p.vx *= -1
      if (p.y < 0 || p.y > canvas.height) p.vy *= -1
      ctx.beginPath()
      ctx.fillStyle = 'rgba(162, 155, 254, 0.6)'
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx.fill()
    })

    bgAnimation = requestAnimationFrame(draw)
  }
  draw()
}

onMounted(() => {
  initBackground()
  nextTick(() => renderGraph())
})

onBeforeUnmount(() => {
  if (chartInstance.value) chartInstance.value.dispose()
  if (resizeGraphHandler) window.removeEventListener('resize', resizeGraphHandler)
  if (bgResizeHandler) window.removeEventListener('resize', bgResizeHandler)
  if (bgAnimation) cancelAnimationFrame(bgAnimation)
})
</script>

<style scoped>
@import url('https://cdn.jsdelivr.net/npm/remixicon@3.5.0/fonts/remixicon.css');
@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;700&display=swap');

:global(:root) {
  --bg-color: transparent;
  --card-bg: rgba(24, 24, 35, 0.6);
  --glass-stroke: rgba(255, 255, 255, 0.08);
  --text-main: #ffffff;
  --text-sub: #9fa6b2;
  --accent: #a29bfe;
}

.nexus-page {
  position: relative;
  min-height: 100vh;
  background: var(--bg-color);
  color: var(--text-main);
  overflow: hidden;
  font-family: 'Space Grotesk', 'SF Pro Display', system-ui, -apple-system, sans-serif;
}

.bg-canvas {
  position: fixed;
  inset: 0;
  z-index: 0;
}

.app-shell {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  padding: 18px 16px 120px;
  box-sizing: border-box;
}

.panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-title {
  margin-top: 6px;
}

.eyebrow {
  font-size: 12px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--text-sub);
  margin: 0 0 4px;
}

.title-row {
  font-size: 24px;
  font-weight: 800;
  display: flex;
  align-items: center;
  gap: 8px;
  background: linear-gradient(120deg, #fff, #a29bfe);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.title-row .dot {
  color: #6c5ce7;
}

.subtitle {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-sub);
  -webkit-text-fill-color: currentColor;
}

.section-header {
  margin-top: 6px;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.section-header.compact {
  margin-top: 0;
}

.section-kicker {
  font-weight: 700;
  margin: 0;
}

.section-sub {
  margin: 2px 0 0;
  color: var(--text-sub);
  font-size: 12px;
}

.glass-card {
  background: var(--card-bg);
  border: 1px solid var(--glass-stroke);
  border-radius: 16px;
  backdrop-filter: blur(10px);
}

.graph-card {
  padding: 12px;
  position: relative;
}

.graph-placeholder {
  width: 100%;
  aspect-ratio: 4/3;
}

.graph-hint {
  position: absolute;
  top: 12px;
  right: 12px;
  font-size: 11px;
  padding: 4px 8px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.05);
  color: var(--text-sub);
  pointer-events: none;
}

.match-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--card-bg);
  border: 1px solid var(--glass-stroke);
  border-radius: 12px;
  transition: all 0.2s;
}

.match-card.active {
  border-color: var(--accent);
  background: rgba(162, 155, 254, 0.1);
}

.avatar-wrap {
  position: relative;
  width: 48px;
  height: 48px;
}

.avatar-wrap img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.badge {
  position: absolute;
  bottom: -4px;
  right: -4px;
  background: #6c5ce7;
  color: #fff;
  font-size: 9px;
  padding: 2px 4px;
  border-radius: 4px;
  font-weight: 700;
}

.match-body {
  flex: 1;
}

.match-name {
  font-weight: 700;
  margin-bottom: 2px;
}

.match-reason {
  font-size: 12px;
  color: var(--text-sub);
  display: flex;
  align-items: center;
  gap: 4px;
}

.match-reason .accent {
  color: var(--accent);
}
</style>
