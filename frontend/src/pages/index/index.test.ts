import { mount, flushPromises } from '@vue/test-utils';
import { describe, it, expect, vi, beforeEach } from 'vitest';

// Mock api 模块：覆盖真正的 fetch 调用
vi.mock('../../api', () => ({
  api: {
    stats: {
      overview: vi.fn(),
    },
  },
}));

// vue3 不能直接定义函数属性类型，从 mock 模块导入
import { api } from '../../api';
import HomePage from './index.vue';
import { uniMock } from '../../../vitest.setup';

describe('HomePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (api.stats.overview as ReturnType<typeof vi.fn>).mockResolvedValue({
      totalClothing: 0,
      totalOutfits: 0,
      monthWears: 0,
    });
  });

  it('shows three stat labels and zero defaults before data loads', async () => {
    (api.stats.overview as ReturnType<typeof vi.fn>).mockImplementation(
      () => new Promise(() => {}), // pending — 验证初始占位
    );
    const wrapper = mount(HomePage);
    const html = wrapper.html();
    expect(html).toContain('衣物');
    expect(html).toContain('搭配');
    expect(html).toContain('本月穿');
    expect(html).toContain('打开衣橱');
    expect(html).toContain('打开日历');
    expect(html).toContain('0');
  });

  it('displays stats from api.stats.overview after mount', async () => {
    (api.stats.overview as ReturnType<typeof vi.fn>).mockResolvedValue({
      totalClothing: 42,
      totalOutfits: 7,
      monthWears: 13,
    });
    const wrapper = mount(HomePage);
    await flushPromises();
    const html = wrapper.html();
    expect(html).toContain('42');
    expect(html).toContain('7');
    expect(html).toContain('13');
  });

  it('falls back to zeros when api.stats.overview rejects', async () => {
    (api.stats.overview as ReturnType<typeof vi.fn>).mockRejectedValue(
      new Error('network'),
    );
    const wrapper = mount(HomePage);
    await flushPromises();
    const html = wrapper.html();
    // 三个 0 都出现
    const zeroMatches = html.match(/0/g) ?? [];
    expect(zeroMatches.length).toBeGreaterThanOrEqual(3);
  });

  it('clicking "打开衣橱" calls uni.navigateTo with closet url', async () => {
    const wrapper = mount(HomePage);
    await flushPromises();
    const buttons = wrapper.findAll('button');
    // 第一个 button 是「打开衣橱」
    await buttons[0].trigger('click');
    expect(uniMock.navigateTo).toHaveBeenCalledTimes(1);
    expect(uniMock.navigateTo).toHaveBeenCalledWith({ url: '/pages/closet/index' });
  });

  it('clicking "打开日历" calls uni.navigateTo with calendar url', async () => {
    const wrapper = mount(HomePage);
    await flushPromises();
    const buttons = wrapper.findAll('button');
    await buttons[1].trigger('click');
    expect(uniMock.navigateTo).toHaveBeenCalledWith({ url: '/pages/calendar/index' });
  });
});
