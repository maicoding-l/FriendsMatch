const js = require('@eslint/js')
const vue = require('eslint-plugin-vue')
const tsParser = require('@typescript-eslint/parser')
const tsPlugin = require('@typescript-eslint/eslint-plugin')
const vueParser = require('vue-eslint-parser')
const prettier = require('eslint-plugin-prettier')
const configPrettier = require('eslint-config-prettier') // ← 新增：关闭各种冲突的“格式化类”规则

module.exports = [
  js.configs.recommended,

  // Vue 扁平推荐配置是一个“数组”，记得展开
  ...vue.configs['flat/recommended'],

  // 这行很关键：把易与 Prettier 冲突的规则全部关掉（包括 vue 的缩进/换行等）
  configPrettier,

  {
    files: ['**/*.{ts,tsx,vue}'],
    languageOptions: {
      parser: vueParser,
      parserOptions: {
        parser: tsParser,
        ecmaVersion: 'latest',
        sourceType: 'module',
        extraFileExtensions: ['.vue']
      },
      globals: {
        showToast: 'readonly'
      }
    },
    plugins: { '@typescript-eslint': tsPlugin, prettier },
    rules: {
      ...tsPlugin.configs.recommended.rules,
      'prettier/prettier': 'warn',

      // 你想放宽或关闭的 Vue 规则
      'vue/multi-word-component-names': 'off',

      // 如果仍有个别提示，按需继续关（可选）
      // 'vue/max-attributes-per-line': 'off',
      // 'vue/html-indent': 'off',
      // 'vue/html-closing-bracket-newline': 'off',
      // 'vue/singleline-html-element-content-newline': 'off',
      // 'vue/multiline-html-element-content-newline': 'off',
      // 'vue/first-attribute-linebreak': 'off',
    }
  },
  { files: ['**/*.d.ts'], rules: { '@typescript-eslint/no-explicit-any': 'off' } },
  { ignores: ['node_modules/**', 'dist/**'] }
]
