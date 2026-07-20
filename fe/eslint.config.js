import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import prettier from 'eslint-config-prettier'
import globals from 'globals'

export default [
  { ignores: ['dist/**', 'node_modules/**'] },

  js.configs.recommended,
  ...pluginVue.configs['flat/recommended'],

  {
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: {
        ...globals.browser, // window, document, localStorage 등 브라우저 전역
      },
    },
  },

  {
    rules: {
      // 페이지/컴포넌트 파일명은 PascalName.vue 단일어도 허용 (HomePage 등)
      'vue/multi-word-component-names': 'off',
      // 미사용 변수는 에러, 단 _ 로 시작하면 무시
      'no-unused-vars': ['error', { argsIgnorePattern: '^_', varsIgnorePattern: '^_' }],
    },
  },

  // prettier 와 충돌하는 포맷 규칙을 끈다 (반드시 마지막)
  prettier,
]
