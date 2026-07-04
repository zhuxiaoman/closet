import { mount, flushPromises } from '@vue/test-utils';
import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('../../api', () => ({
  api: {
    clothing: { list: vi.fn() },
  },
}));

import { api } from '../../api';
import ClosetPage from './index.vue';
import { uniMock } from '../../../vitest.setup';

describe('ClosetPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (api.clothing.list as ReturnType<typeof vi.fn>).mockResolvedValue({ records: [], total: 0 });
  });

  it('renders header + add button and triggers fetchList on mount', async () => {
    const wrapper = mount(ClosetPage);
    await flushPromises();
    const html = wrapper.html();
    expect(html).toContain('+ 添加衣物');
    expect(html).toContain('暂无衣物');
    expect(api.clothing.list).toHaveBeenCalledTimes(1);
  });

  it('clicking "+ 添加衣物" navigates to clothing-form page', async () => {
    const wrapper = mount(ClosetPage);
    await flushPromises();
    const buttons = wrapper.findAll('button');
    await buttons[0].trigger('click');
    expect(uniMock.navigateTo).toHaveBeenCalledTimes(1);
    expect(uniMock.navigateTo).toHaveBeenCalledWith({
      url: '/pages/clothing-form/index',
    });
  });

  it('clicking a clothing card navigates to detail page with id query', async () => {
    (api.clothing.list as ReturnType<typeof vi.fn>).mockResolvedValue({
      records: [
        { id: 7, name: 'T1' },
        { id: 11, name: 'T2' },
      ],
      total: 2,
    });
    const wrapper = mount(ClosetPage);
    await flushPromises();
    const cards = wrapper.findAllComponents({ name: 'ClothingCard' });
    expect(cards.length).toBe(2);
    await cards[0].trigger('click');
    expect(uniMock.navigateTo).toHaveBeenCalledWith({
      url: '/pages/clothing-detail/index?id=7',
    });
  });

  it('filter change event re-triggers fetchList with merged filter', async () => {
    const wrapper = mount(ClosetPage);
    await flushPromises();
    expect(api.clothing.list).toHaveBeenCalledTimes(1);

    const filterBar = wrapper.findComponent({ name: 'FilterBar' });
    expect(filterBar.exists()).toBe(true);
    filterBar.vm.$emit('change', { season: 'summer', keyword: 'k' });
    await flushPromises();

    expect(api.clothing.list).toHaveBeenCalledTimes(2);
    const lastCall = (api.clothing.list as ReturnType<typeof vi.fn>).mock.calls.at(-1)?.[0] as {
      season: string | null;
      keyword?: string;
    };
    expect(lastCall.season).toBe('summer');
    expect(lastCall.keyword).toBe('k');
  });

  it('keeps empty state when fetchList rejects', async () => {
    (api.clothing.list as ReturnType<typeof vi.fn>).mockRejectedValueOnce(new Error('network'));
    const wrapper = mount(ClosetPage);
    await flushPromises();
    const html = wrapper.html();
    expect(html).toContain('暂无衣物');
  });
});
