import { config } from '@vue/test-utils';
import { h, defineComponent } from 'vue';

// uni-app 标签 -> 原生 HTML 元素映射。
// 必须用 h() render function，不能用 template: '<img><slot/></img>'
// （happy-dom 对自闭合 img 标签的 template 解析有问题）。
// 组件的 name 用 'UniXxx' 前缀避开和原生 HTML 元素名冲突。
const passthrough = (htmlTag: 'img' | 'div' | 'span', name: string) =>
  defineComponent({
    name,
    setup(_, { slots, attrs }) {
      return () => h(htmlTag, attrs, slots.default ? slots.default() : undefined);
    },
  });

config.global.components = {
  ...(config.global.components as Record<string, unknown>),
  image: passthrough('img', 'UniImage'),
  view: passthrough('div', 'UniView'),
  text: passthrough('span', 'UniText'),
  'scroll-view': passthrough('div', 'UniScrollView'),
};
