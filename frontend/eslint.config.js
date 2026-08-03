import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import globals from 'globals'

export default [
  { ignores: ['dist/**', 'node_modules/**', 'public/**'] },
  js.configs.recommended,
  ...pluginVue.configs['flat/essential'],
  {
    files: ['src/**/*.{js,vue}', 'vite.config.js'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: {
        ...globals.browser,
        ...globals.node
      }
    },
    rules: {
      // 最小机械检查：仅拦截真实错误，不做风格约束
      'no-unused-vars': ['warn', { args: 'none' }],
      'vue/multi-word-component-names': 'off',
      // 项目约定以 console.warn/error 输出诊断日志（见提交记录「静默 catch 统一改为 console.warn」），
      // 因此仅放行 warn/error，console.log/debug 仍按警告拦截
      'no-console': ['warn', { allow: ['warn', 'error'] }],
      'no-debugger': 'error',
      'eqeqeq': ['warn', 'always', { null: 'ignore' }],
      'no-var': 'error',
      'prefer-const': 'warn'
    }
  }
]
