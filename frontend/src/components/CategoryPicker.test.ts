import { mount } from '@vue/test-utils';
import { describe, it, expect } from 'vitest';
import CategoryPicker from './CategoryPicker.vue';

describe('CategoryPicker', () => {
  const categories = [
    { id: 1, name: '上装' },
    { id: 2, name: '下装' },
    { id: 3, name: '外套' },
  ];
  const stubs = {
    'up-checkbox-group': true,
    'up-checkbox': true,
  } as const;
  // 真实 uView Plus 组件通过 vitest.setup.ts 全局注册成 stub，本文件不再需要 global.stubs

  it('renders one checkbox per category', () => {
    const wrapper = mount(CategoryPicker, {
      props: { categories, modelValue: [] },
    });
    const html = wrapper.html();
    expect(html).toContain('上装');
    expect(html).toContain('下装');
    expect(html).toContain('外套');
    expect(wrapper.findAllComponents({ name: 'UpCheckboxStub' }).length).toBe(3);
  });

  it('renders nothing when categories list is empty', () => {
    const wrapper = mount(CategoryPicker, {
      props: { categories: [], modelValue: [] },
    });
    expect(wrapper.findAllComponents({ name: 'UpCheckboxStub' }).length).toBe(0);
  });

  it('initializes internal state from modelValue', () => {
    const wrapper = mount(CategoryPicker, {
      props: { categories, modelValue: [1, 3] },
    });
    expect((wrapper.vm as unknown as { selected: number[] }).selected).toEqual([1, 3]);
  });

  it('does not emit update:modelValue on mount with initial value', () => {
    const wrapper = mount(CategoryPicker, {
      props: { categories, modelValue: [1] },
    });
    expect(wrapper.emitted('update:modelValue')).toBeUndefined();
  });

  it('reacts to external modelValue changes', async () => {
    const wrapper = mount(CategoryPicker, {
      props: { categories, modelValue: [1] },
    });
    await wrapper.setProps({ modelValue: [2, 3] });
    expect((wrapper.vm as unknown as { selected: number[] }).selected).toEqual([2, 3]);
  });
});
