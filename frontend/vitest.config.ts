import { defineConfig } from 'vitest/config';
import vue from '@vitejs/plugin-vue';

// Vitest config: 让 .vue 文件能被编译，并告诉编译器 <view>/<text>/<scroll-view>
// 是 uni-app 组件（不当作原生 HTML 元素）。<image> 不在列表里 —— 它由
// vitest.setup.ts 注册为全局组件并 render 成 <img>。happy-dom 足够 mount SFC。
export default defineConfig({
  plugins: [
    vue({
      template: {
        compilerOptions: {
          isCustomElement: (tag: string) =>
            ['view', 'text', 'scroll-view'].includes(tag),
        },
      },
    }),
  ],
  test: {
    environment: 'happy-dom',
    setupFiles: ['./vitest.setup.ts'],
  },
});
