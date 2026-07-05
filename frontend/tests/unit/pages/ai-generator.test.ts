import { describe, it, expect, beforeEach, vi } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import { setActivePinia, createPinia } from 'pinia';
import AiGeneratorPage from '@/pages/ai-generator/ai-generator.vue';
import { api } from '@/api';

// 透传 children slots 的 stub，避免引入真实组件的副作用
const AiGeneratorStub = {
  template: '<view class="ai-stub"><slot /></view>',
  props: ['seedIds', 'generating'],
};
const OutfitResultCardStub = {
  template: '<view class="result-stub">{{ items }}</view>',
  props: ['items', 'index', 'generationId'],
};

describe('ai-generator page', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    // mock api.clothing.list 避免走真实 fetch
    vi.spyOn(api.clothing, 'list').mockResolvedValue([
      { id: 1, name: '白衬衫', imageUrl: '/a.jpg', category: 'top', color: 'white' },
      { id: 2, name: '牛仔裤', imageUrl: '/b.jpg', category: 'bottom', color: 'blue' },
    ] as never);
  });

  it('显示"选择起点单品"提示当未选起点', async () => {
    const wrapper = mount(AiGeneratorPage, {
      global: {
        stubs: {
          AiGenerator: AiGeneratorStub,
          OutfitResultCard: OutfitResultCardStub,
        },
      },
    });
    // 等 onMounted 拉衣橱列表完成
    await flushPromises();
    expect(wrapper.html()).toContain('选择起点单品');
  });
});
