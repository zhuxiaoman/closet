import { mount, flushPromises } from '@vue/test-utils';
import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('../../api', () => ({
  api: {
    outfits: { list: vi.fn() },
  },
}));

import { api } from '../../api';
import OutfitsPage from './index.vue';
import { uniMock } from '../../../vitest.setup';

describe('OutfitsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (api.outfits.list as ReturnType<typeof vi.fn>).mockResolvedValue({ records: [], total: 0 });
  });

  it('renders add button and empty hint on first mount, triggers fetchList', async () => {
    const wrapper = mount(OutfitsPage);
    await flushPromises();
    const html = wrapper.html();
    expect(html).toContain('+ 新建搭配');
    expect(html).toContain('还没有搭配');
    expect(api.outfits.list).toHaveBeenCalledTimes(1);
  });

  it('clicking "+ 新建搭配" navigates to outfit-form page', async () => {
    const wrapper = mount(OutfitsPage);
    await flushPromises();
    const buttons = wrapper.findAll('button');
    const addBtn = buttons.find((b) => b.text().includes('+ 新建搭配'));
    expect(addBtn).toBeTruthy();
    await addBtn!.trigger('click');
    expect(uniMock.navigateTo).toHaveBeenCalledTimes(1);
    expect(uniMock.navigateTo).toHaveBeenCalledWith({
      url: '/pages/outfit-form/index',
    });
  });

  it('renders cards for each outfit and clicking one navigates to detail', async () => {
    (api.outfits.list as ReturnType<typeof vi.fn>).mockResolvedValue({
      records: [
        { id: 7, name: '周末休闲', description: '逛街咖啡', itemCount: 3 },
        { id: 11, name: '通勤', description: '', itemCount: 2 },
      ],
      total: 2,
    });
    const wrapper = mount(OutfitsPage);
    await flushPromises();
    const html = wrapper.html();
    expect(html).toContain('周末休闲');
    expect(html).toContain('包含 3 件衣物');
    expect(html).toContain('包含 2 件衣物');

    const cards = wrapper.findAll('[data-test="outfit-card"]');
    expect(cards.length).toBe(2);
    await cards[0].trigger('click');
    expect(uniMock.navigateTo).toHaveBeenCalledWith({
      url: '/pages/outfit-detail/index?id=7',
    });
  });

  it('keeps empty state when fetchList rejects', async () => {
    (api.outfits.list as ReturnType<typeof vi.fn>).mockRejectedValueOnce(new Error('network'));
    const wrapper = mount(OutfitsPage);
    await flushPromises();
    expect(wrapper.html()).toContain('还没有搭配');
  });
});
