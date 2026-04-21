# Friends Finder 设计系统文档

## 概述

本设计系统为 Friends Finder 应用提供统一的视觉语言和交互模式，确保整个应用的一致性和可维护性。

## 核心原则

1. **可访问性优先** - 所有组件符合 WCAG 2.1 AA 标准
2. **性能优化** - 使用 CSS 变量和现代 CSS 特性
3. **响应式设计** - 移动优先，流式布局
4. **渐进增强** - 基础功能在所有设备上可用

## 设计令牌 (Design Tokens)

所有设计令牌定义在 `src/styles/design-tokens.css`

### 颜色系统

使用 OKLCH 颜色空间以确保感知一致性：

- **中性色**: `--color-neutral-{50-950}` - 带有轻微紫色调的中性色
- **主色**: `--color-primary-{50-900}` - 深靛蓝色
- **强调色**: `--color-accent-{50-700}` - 暖珊瑚色（用于操作按钮）
- **语义色**: success, warning, error, info

**为什么移除 AI 色板？**
- 之前的 `#a29bfe` 紫色是典型的 AI 生成配色
- 新的深靛蓝 (280度色相) 更专业、更独特
- 珊瑚色强调色 (30度色相) 提供更好的视觉对比

### 排版系统

- **字体族**:
  - 主体: Inter Variable (替代 Space Grotesk)
  - 展示: Newsreader (衬线字体，用于标题)
  - 等宽: JetBrains Mono

- **字体大小**: 使用 `clamp()` 实现流式排版
  - `--font-size-xs` 到 `--font-size-3xl`
  - 自动在不同视口尺寸间缩放

### 间距系统

基于 8px 网格系统：
- 固定间距: `--space-{0-24}` (4px, 8px, 12px, 16px...)
- 流式间距: `--space-fluid-{xs-2xl}` (使用 clamp())

### 圆角

- `--radius-{sm-2xl}` 和 `--radius-full`
- 避免过度使用圆角

### 阴影

- `--shadow-{xs-xl}` - 层次阴影
- `--shadow-glow` - 用于特殊强调（谨慎使用）

## 组件模式

所有可复用组件样式定义在 `src/styles/components.css`

### 卡片 (Cards)

```css
.card               /* 基础卡片 */
.card--interactive  /* 可交互卡片 */
.card--glass        /* 玻璃效果卡片（谨慎使用）*/
```

**避免事项**:
- 不要嵌套卡片
- 不要所有内容都用卡片包裹
- 玻璃效果仅在需要层次时使用

### 按钮 (Buttons)

```css
.btn
.btn--primary
.btn--secondary
.btn--ghost
.btn--accent
```

**尺寸变体**: `--sm`, `--lg`
**修饰符**: `--full`, `--rounded`

### 页面头部 (Page Headers)

```css
.page-header
.page-header__eyebrow
.page-header__title
.page-header__subtitle
```

**不要**：
- 所有页面使用相同的头部模式
- 使用渐变文字作为装饰
- 在标题上使用 eyebrow 标签（除非真正需要）

### 布局系统

- `.grid`, `.grid--{2-4}` - 网格布局
- `.stack--{xs-xl}` - 垂直堆叠
- `.cluster--{xs-lg}` - 水平排列

## 迁移指南

### 从旧样式迁移到设计系统

#### 1. 移除字体导入

❌ 旧方式:
```css
@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk...');
```

✅ 新方式:
字体已在 `base.css` 中全局加载

#### 2. 使用设计令牌替代硬编码颜色

❌ 旧方式:
```css
color: #a29bfe;
background: rgba(24, 24, 35, 0.6);
```

✅ 新方式:
```css
color: var(--color-primary-400);
background: var(--color-surface-raised);
```

#### 3. 使用间距令牌

❌ 旧方式:
```css
padding: 20px;
margin-top: 24px;
gap: 16px;
```

✅ 新方式:
```css
padding: var(--space-5);
margin-top: var(--space-6);
gap: var(--space-4);
```

#### 4. 移除渐变文字

❌ 旧方式:
```css
background: linear-gradient(120deg, #fff, #a29bfe);
-webkit-background-clip: text;
-webkit-text-fill-color: transparent;
```

✅ 新方式:
```css
color: var(--color-text-primary);
/* 如果需要强调，使用: */
color: var(--color-primary-300);
```

#### 5. 简化毛玻璃效果

❌ 旧方式:
```css
background: rgba(24, 24, 35, 0.6);
backdrop-filter: blur(10px);
border: 1px solid rgba(255, 255, 255, 0.08);
```

✅ 新方式:
```css
/* 仅在真正需要层次时使用 */
background: var(--glass-bg);
backdrop-filter: blur(var(--glass-blur));
border: 1px solid var(--glass-border);

/* 大多数情况下使用普通卡片 */
background: var(--color-surface-raised);
border: 1px solid var(--color-border-subtle);
```

## 可访问性检查清单

在创建或修改组件时：

- [ ] 所有交互元素有焦点指示器
- [ ] 可点击区域至少 44x44px
- [ ] 颜色对比度符合 WCAG AA (4.5:1)
- [ ] 图片有有意义的 alt 文本
- [ ] 键盘可访问 (tabindex, 键盘事件)
- [ ] 使用语义化 HTML
- [ ] 加载状态有 aria-live 通知
- [ ] 表单有关联的标签

## 性能优化

- 字体仅加载一次（在 base.css）
- 图片使用懒加载
- 避免昂贵的 CSS 操作（如过度使用 blur）
- 使用 CSS 变量而非内联样式

## 主题支持

当前应用使用深色主题。设计令牌系统已为浅色主题预留了支持：

```html
<div data-theme="dark">  <!-- 当前 -->
<div data-theme="light"> <!-- 未来 -->
```

## 文件结构

```
src/
├── styles/
│   ├── design-tokens.css  # 设计令牌定义
│   ├── base.css           # 全局样式和重置
│   └── components.css     # 可复用组件样式
├── pages/
│   └── *.vue              # 页面组件（使用设计令牌）
└── components/
    └── *.vue              # 通用组件（使用设计令牌）
```

## 工具和资源

- **颜色**: [OKLCH Color Picker](https://oklch.com)
- **对比度检查**: [WebAIM Contrast Checker](https://webaim.org/resources/contrastchecker/)
- **字体**: [Google Fonts](https://fonts.google.com)
- **图标**: RemixIcon (已集成)

## 贡献指南

在添加新样式时：

1. 检查是否已有设计令牌可用
2. 如果需要新令牌，添加到 `design-tokens.css`
3. 如果是可复用组件，添加到 `components.css`
4. 页面特定样式保留在组件的 `<style scoped>` 中
5. 确保符合可访问性标准

## 未来改进

- [ ] 添加亮色主题支持
- [ ] 建立 Storybook 组件库
- [ ] 添加动画库
- [ ] 创建表单组件系统
- [ ] 添加图表主题
