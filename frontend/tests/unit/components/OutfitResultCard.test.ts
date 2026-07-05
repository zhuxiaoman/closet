import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import OutfitResultCard from '@/components/OutfitResultCard.vue';

describe('OutfitResultCard', () => {
  it('渲染所有单品名称', () => {
    const wrapper = mount(OutfitResultCard, {
      props: {
        items: ['白衬衫', '牛仔裤', '运动鞋'],
        index: 1,
        generationId: 42,
      },
    });
    expect(wrapper.html()).toContain('白衬衫');
    expect(wrapper.html()).toContain('牛仔裤');
    expect(wrapper.html()).toContain('运动鞋');
  });

  it('emit like 事件当点击 like 按钮', async () => {
    const wrapper = mount(OutfitResultCard, {
      props: {
        items: ['A', 'B'],
        index: 2,
        generationId: 42,
      },
    });
    await wrapper.find('[data-test="like"]').trigger('click');
    expect(wrapper.emitted('like')).toBeTruthy();
    expect(wrapper.emitted('like')!.length).toBe(1);
  });
});
