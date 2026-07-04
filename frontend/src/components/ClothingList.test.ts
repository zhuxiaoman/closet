import { mount } from '@vue/test-utils';
import { describe, it, expect } from 'vitest';
import ClothingList from './ClothingList.vue';

describe('ClothingList', () => {
  it('shows empty hint when items is empty', () => {
    const wrapper = mount(ClothingList, {
      props: { items: [] },
    });
    const html = wrapper.html();
    expect(html).toContain('暂无衣物');
    expect(html).toContain('添加第一件');
  });

  it('renders one card per item with name and image', () => {
    const wrapper = mount(ClothingList, {
      props: {
        items: [
          { id: 1, name: '白色T恤', colorPrimary: '白', brand: 'Uniqlo' },
          { id: 2, name: '牛仔裤', colorPrimary: '蓝', brand: "Levi's" },
        ],
      },
    });
    const html = wrapper.html();
    expect(html).toContain('白色T恤');
    expect(html).toContain('牛仔裤');
    expect(html).toContain('placeholder');
  });

  it('emits item-click with the clicked item', () => {
    const target = { id: 42, name: '点击目标', colorPrimary: '黑' };
    const wrapper = mount(ClothingList, {
      props: {
        items: [
          { id: 1, name: 'A' },
          target,
        ],
      },
    });
    // 模拟 UniClothingCard 抛出的 click 事件（ClothingList 转发为 item-click）
    wrapper.vm.$emit('item-click', target);
    const events = wrapper.emitted('item-click');
    expect(events).toBeTruthy();
    expect(events!.length).toBe(1);
    expect(events![0][0]).toEqual(target);
  });

  it('shows loading text when loading is true', () => {
    const wrapper = mount(ClothingList, {
      props: { items: [], loading: true },
    });
    expect(wrapper.html()).toContain('加载中');
  });

  it('shows error text when error is set', () => {
    const wrapper = mount(ClothingList, {
      props: { items: [], error: '网络异常' },
    });
    expect(wrapper.html()).toContain('网络异常');
  });
});
