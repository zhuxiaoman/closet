import { describe, it, expect, beforeEach, vi } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import { setActivePinia, createPinia } from 'pinia';
import IndexPage from '@/pages/index/index.vue';
import { api } from '@/api';

// uni-app 全局对象在 happy-dom 下未定义，手动 stub 出首页用到的子集
beforeEach(() => {
  setActivePinia(createPinia());
  (globalThis as unknown as { uni: unknown }).uni = {
    navigateTo: vi.fn(),
    showToast: vi.fn(),
  };
  // 网络错误也无所谓，首页只在 catch 里吞掉
  vi.spyOn(api.stats, 'overview').mockResolvedValue({
    totalClothing: 0,
    totalOutfits: 0,
    monthWears: 0,
  } as never);
});

describe('首页 AI 入口', () => {
  it('渲染 ai-cta 卡片,带 data-test="ai-cta"', async () => {
    const wrapper = mount(IndexPage);
    await flushPromises();
    const cta = wrapper.find('[data-test="ai-cta"]');
    expect(cta.exists()).toBe(true);
    expect(cta.text()).toContain('AI 搭配助手');
    expect(cta.text()).toContain('挑件单品，生成 5 套方案');
  });

  it('点击 ai-cta 触发 navigateTo 跳转到 ai-generator', async () => {
    const wrapper = mount(IndexPage);
    await flushPromises();
    await wrapper.find('[data-test="ai-cta"]').trigger('click');
    const uni = (globalThis as unknown as {
      uni: { navigateTo: ReturnType<typeof vi.fn> };
    }).uni;
    expect(uni.navigateTo).toHaveBeenCalledWith({
      url: '/pages/ai-generator/ai-generator',
    });
  });
});