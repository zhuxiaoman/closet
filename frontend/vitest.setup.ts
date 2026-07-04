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

// uView Plus 在测试环境下用 stub 替代，避免引入 uview-plus 的运行时依赖。
// 真实运行时 uView Plus 自己注册这些组件；本 stub 仅在 vitest 环境被全局注册。
const upCheckboxGroupStub = defineComponent({
  name: 'UpCheckboxGroupStub',
  setup(_, { slots, attrs }) {
    return () =>
      h('div', { class: 'up-checkbox-group-stub', ...attrs }, slots.default?.());
  },
});

const upCheckboxStub = defineComponent({
  name: 'UpCheckboxStub',
  props: ['label', 'value'],
  setup(props, { slots }) {
    const children = [props.label, slots.default?.()].filter(Boolean);
    return () =>
      h(
        'label',
        { class: 'up-checkbox-stub', 'data-value': String(props.value ?? '') },
        children,
      );
  },
});

config.global.components = {
  ...(config.global.components as Record<string, unknown>),
  image: passthrough('img', 'UniImage'),
  view: passthrough('div', 'UniView'),
  text: passthrough('span', 'UniText'),
  'scroll-view': passthrough('div', 'UniScrollView'),
  picker: passthrough('div', 'UniPicker'),
  'up-checkbox-group': upCheckboxGroupStub,
  'up-checkbox': upCheckboxStub,
};
