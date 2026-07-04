import { mount } from '@vue/test-utils';
import { describe, it, expect } from 'vitest';
import TagPicker from './TagPicker.vue';

describe('TagPicker', () => {
  const tags = [
    { id: 10, name: '基础款' },
    { id: 20, name: '通勤' },
    { id: 30, name: '运动' },
  ];
  const stubs = {
    'up-checkbox-group': true,
    'up-checkbox': true,
  } as const;
  // 真实 uView Plus 组件通过 vitest.setup.ts 全局注册成 stub，本文件不再需要 global.stubs

  it('renders one checkbox per tag', () => {
    const wrapper = mount(TagPicker, {
      props: { tags, modelValue: [] },
    });
    const html = wrapper.html();
    expect(html).toContain('基础款');
    expect(html).toContain('通勤');
    expect(html).toContain('运动');
    expect(wrapper.findAllComponents({ name: 'UpCheckboxStub' }).length).toBe(3);
  });

  it('renders nothing when tags list is empty', () => {
    const wrapper = mount(TagPicker, {
      props: { tags: [], modelValue: [] },
    });
    expect(wrapper.findAllComponents({ name: 'UpCheckboxStub' }).length).toBe(0);
  });

  it('initializes internal state from modelValue', () => {
    const wrapper = mount(TagPicker, {
      props: { tags, modelValue: [10, 30] },
    });
    expect((wrapper.vm as unknown as { selected: number[] }).selected).toEqual([10, 30]);
  });

  it('does not emit update:modelValue on mount with initial value', () => {
    const wrapper = mount(TagPicker, {
      props: { tags, modelValue: [10] },
    });
    expect(wrapper.emitted('update:modelValue')).toBeUndefined();
  });

  it('reacts to external modelValue changes', async () => {
    const wrapper = mount(TagPicker, {
      props: { tags, modelValue: [10] },
    });
    await wrapper.setProps({ modelValue: [20, 30] });
    expect((wrapper.vm as unknown as { selected: number[] }).selected).toEqual([20, 30]);
  });
});
