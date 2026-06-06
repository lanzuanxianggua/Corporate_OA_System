import pluginVue from 'eslint-plugin-vue'
import vueTsEslintConfig from '@vue/eslint-config-typescript'
import skipFormatting from '@vue/eslint-config-prettier/skip-formatting'

// Flat config 模式（ESLint 9.x）。Vue 3 + TypeScript + Prettier 兼容。
// 参考：https://eslint.vuejs.org/user-guide/

export default [
  {
    name: 'app/files-to-lint',
    files: ['**/*.{vue,ts,tsx}'],
  },
  {
    name: 'app/files-to-ignore',
    ignores: [
      '**/dist/**',
      '**/dist-ssr/**',
      '**/coverage/**',
      '**/node_modules/**',
      '**/playwright-report/**',
      '**/test-results/**',
      '**/*.config.{js,ts,mjs,cjs}',  // Vite/Vitest/Playwright 等配置文件（自带类型检查）
    ],
  },
  ...pluginVue.configs['flat/essential'],
  ...vueTsEslintConfig(),
  skipFormatting,
  {
    rules: {
      // 关闭一些对当前代码库过严的规则，按需开启
      '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
      '@typescript-eslint/no-explicit-any': 'warn',
      '@typescript-eslint/no-empty-object-type': 'off', // env.d.ts / 类型合并处会出现 `interface X {}`
      'vue/multi-word-component-names': 'off',
      'vue/no-v-html': 'off',
    },
  },
]
