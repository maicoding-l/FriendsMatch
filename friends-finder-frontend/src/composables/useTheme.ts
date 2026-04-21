import { ref, watch, onMounted } from 'vue'

type Theme = 'light' | 'dark'

const THEME_KEY = 'friends-finder-theme'

// 全局主题状态
const currentTheme = ref<Theme>('light')

export function useTheme() {
  // 设置主题
  const setTheme = (theme: Theme) => {
    currentTheme.value = theme
    document.documentElement.setAttribute('data-theme', theme)
    localStorage.setItem(THEME_KEY, theme)
  }

  // 切换主题
  const toggleTheme = () => {
    const newTheme = currentTheme.value === 'light' ? 'dark' : 'light'
    setTheme(newTheme)
  }

  // 初始化主题
  const initTheme = () => {
    // 优先使用localStorage中的设置
    const savedTheme = localStorage.getItem(THEME_KEY) as Theme | null

    if (savedTheme) {
      setTheme(savedTheme)
    } else {
      // 默认使用浅色主题
      setTheme('light')
    }
  }

  // 监听系统主题变化（可选）
  const watchSystemTheme = () => {
    if (window.matchMedia) {
      const darkModeQuery = window.matchMedia('(prefers-color-scheme: dark)')
      darkModeQuery.addEventListener('change', (e) => {
        // 只在用户没有手动设置过主题时才跟随系统
        if (!localStorage.getItem(THEME_KEY)) {
          setTheme(e.matches ? 'dark' : 'light')
        }
      })
    }
  }

  return {
    currentTheme,
    setTheme,
    toggleTheme,
    initTheme,
    watchSystemTheme,
  }
}
