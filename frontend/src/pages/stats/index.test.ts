import { mount, flushPromises } from '@vue/test-utils';
import { describe, it, expect, vi, beforeEach } from 'vitest';

// mock api 模块：用 vi.fn() 替换 fetch 链路
vi.mock('../../api', () => ({
  api: {
    stats: {
      overview: vi.fn(),
      mostWorn: vi.fn(),
      leastWorn: vi.fn(),
    },
  },
}));

import { api } from '../../api';
import StatsPage from './index.vue';
import { uniMock } from '../../../vitest.setup';

describe('StatsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (api.stats.overview as ReturnType<typeof vi.fn>).mockResolvedValue({
      totalClothing: 0,
      totalOutfits: 0,
      monthWears: 0,
    });
    (api.stats.mostWorn as ReturnType<typeof vi.fn>).mockResolvedValue([]);
    (api.stats.leastWorn as ReturnType<typeof vi.fn>).mockResolvedValue([]);
  });

  it('renders the four section titles even before data resolves', () => {
    (api.stats.overview as ReturnType<typeof vi.fn>).mockImplementation(
      () => new Promise(() => {}), // pending：验证初始占位
    );
    (api.stats.mostWorn as ReturnType<typeof vi.fn>).mockImplementation(
      () => new Promise(() => {}),
    );
    (api.stats.leastWorn as ReturnType<typeof vi.fn>).mockImplementation(
      () => new Promise(() => {}),
    );
    const wrapper = mount(StatsPage);
    const html = wrapper.html();
    expect(html).toContain('衣物');
    expect(html).toContain('搭配');
    expect(html).toContain('本月穿');
    expect(html).toContain('最常穿 Top 10');
    expect(html).toContain('最少穿');
  });

  it('displays overview numbers after api.stats.overview resolves', async () => {
    (api.stats.overview as ReturnType<typeof vi.fn>).mockResolvedValue({
      totalClothing: 42,
      totalOutfits: 7,
      monthWears: 13,
    });
    (api.stats.mostWorn as ReturnType<typeof vi.fn>).mockResolvedValue([
      { clothingId: 1, name: '白衬衫', wearCount: 9 },
      { clothingId: 2, name: '牛仔裤', wearCount: 5 },
    ]);
    (api.stats.leastWorn as ReturnType<typeof vi.fn>).mockResolvedValue([
      { clothingId: 3, name: '风衣', daysSinceLastWear: 120 },
    ]);
    const wrapper = mount(StatsPage);
    await flushPromises();
    const html = wrapper.html();
    expect(html).toContain('42');
    expect(html).toContain('7');
    expect(html).toContain('13');
    // 最常穿条目
    expect(html).toContain('白衬衫');
    expect(html).toContain('9 次');
    expect(html).toContain('牛仔裤');
    expect(html).toContain('5 次');
    // 最少穿条目
    expect(html).toContain('风衣');
    expect(html).toContain('120 天前穿');
  });

  it('clicking a most-worn row navigates to clothing detail with id', async () => {
    (api.stats.mostWorn as ReturnType<typeof vi.fn>).mockResolvedValue([
      { clothingId: 88, name: '小黑裙', wearCount: 12 },
    ]);
    const wrapper = mount(StatsPage);
    await flushPromises();
    // 触发最常穿列表项的 click
    const rows = wrapper.findAll('.list-item');
    expect(rows.length).toBeGreaterThan(0);
    await rows[0].trigger('click');
    expect(uniMock.navigateTo).toHaveBeenCalledTimes(1);
    expect(uniMock.navigateTo).toHaveBeenCalledWith({
      url: '/pages/clothing-detail/index?id=88',
    });
  });

  it('falls back to zeros / empty list when api rejects', async () => {
    (api.stats.overview as ReturnType<typeof vi.fn>).mockRejectedValue(
      new Error('network'),
    );
    (api.stats.mostWorn as ReturnType<typeof vi.fn>).mockRejectedValue(
      new Error('network'),
    );
    (api.stats.leastWorn as ReturnType<typeof vi.fn>).mockRejectedValue(
      new Error('network'),
    );
    const wrapper = mount(StatsPage);
    await flushPromises();
    const html = wrapper.html();
    // 三个 0 占位
    const zeroMatches = html.match(/>0</g) ?? [];
    expect(zeroMatches.length).toBeGreaterThanOrEqual(3);
    // 空状态
    expect(html).toContain('暂无数据');
  });
});

