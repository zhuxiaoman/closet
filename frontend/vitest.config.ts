import { defineConfig } from 'vitest/config';
import vue from '@vitejs/plugin-vue';
import { fileURLToPath } from 'node:url';

// Vitest config: 让 .vue 文件能被编译，并告诉编译器 <view>/<text>/<scroll-view>
// 是 uni-app 组件（不当作原生 HTML 元素）。<image> 不在列表里 —— 它由
// vitest.setup.ts 注册为全局组件并 render 成 <img>。happy-dom 够 mount SFC。
//
// resolve.alias 镜像 tsconfig 的 @/* -> src/*，让 tests/unit/ 下的测试
// 也能用绝对 alias 引用源码（src/components/*.test.ts 走相对路径不受影响）。
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
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  test: {
    environment: 'happy-dom',
    setupFiles: ['./vitest.setup.ts'],
  },
});