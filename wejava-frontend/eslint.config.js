import js from '@eslint/js'
import tseslint from 'typescript-eslint'
import pluginVue from 'eslint-plugin-vue'
import globals from 'globals'

export default tseslint.config(
  {
    // 构建产物、依赖、自动生成的类型声明不参与检查
    ignores: ['dist/**', 'node_modules/**', 'src/types/components.d.ts', 'auto-imports.d.ts']
  },

  js.configs.recommended,
  ...tseslint.configs.recommended,
  // essential 只包含正确性规则，不包含排版规则（排版由现有代码风格决定）
  ...pluginVue.configs['flat/essential'],

  {
    files: ['**/*.vue'],
    languageOptions: {
      parserOptions: { parser: tseslint.parser }
    }
  },

  {
    // Crud 组件接收的是 useCrud 返回的共享响应式实例（相当于一个轻量 store），
    // 子组件直接改它的字段是本项目的既定设计，这里关掉 prop 变更检查
    files: ['src/components/Crud/**/*.vue'],
    rules: { 'vue/no-mutating-props': 'off' }
  },

  {
    languageOptions: {
      globals: { ...globals.browser, ...globals.node }
    },
    rules: {
      // Vue API 由 unplugin-auto-import 自动引入，且 TS 已经会检查未定义变量，这里关掉避免误报
      'no-undef': 'off',
      // 组件名用单个单词（index.vue、SidebarItem.vue）是项目约定
      'vue/multi-word-component-names': 'off',
      // 属性顺序交给格式化，不强制
      'vue/attributes-order': 'off',
      'vue/max-attributes-per-line': 'off',
      'vue/singleline-html-element-content-newline': 'off',
      'vue/html-self-closing': 'off',
      'vue/html-indent': 'off',
      // 未使用变量报错，但允许 _ 开头的占位参数
      '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_', varsIgnorePattern: '^_' }],
      // any 先作为警告，后续逐步收敛
      '@typescript-eslint/no-explicit-any': 'warn',
      'no-empty': ['error', { allowEmptyCatch: true }]
    }
  }
)
