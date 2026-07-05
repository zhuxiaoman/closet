import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import AiGenerator from '@/components/AiGenerator.vue';

describe('AiGenerator', () => {
  it('显示加载态当 generating=true', () => {
    const wrapper = mount(AiGenerator, {
      props: { seedIds: [1, 2], generating: true },
    });
    expect(wrapper.html()).toContain('生成中');
  });

  it('emit generate 事件当点击按钮', async () => {
    const wrapper = mount(AiGenerator, {
      props: { seedIds: [1, 2], generating: false },
    });
    await wrapper.find('[data-test="ai-go"]').trigger('click');
    expect(wrapper.emitted('generate')).toBeTruthy();
    expect(wrapper.emitted('generate')![0]).toEqual([
      { seedIds: [1, 2], occasion: 'casual', season: 'all' },
    ]);
  });
});
