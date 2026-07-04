import { mount, flushPromises } from '@vue/test-utils';
import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('../../api', () => ({
  api: {
    outfits: {
      get: vi.fn(),
      delete: vi.fn(),
    },
  },
}));

import { api } from '../../api';
import DetailPage from './index.vue';
import { uniMock } from '../../../vitest.setup';

const apiMock = api as unknown as {
  outfits: { get: ReturnType<typeof vi.fn>; delete: ReturnType<typeof vi.fn> };
};

function setOutfitId(id: number | null) {
  (globalThis as Record<string, unknown>).__closetOutfitDetailId = id;
}

describe('OutfitDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    setOutfitId(null);
  });

  it('mounts with id, fetches detail and renders content', async () => {
    setOutfitId(7);
    apiMock.outfits.get.mockResolvedValue({
      id: 7,
      name: '周末休闲',
      description: '逛街咖啡',
      occasion: '周末',
      season: '夏',
      isFavorite: true,
      items: [
        { id: 1, name: '小黑裙', mainImageKey: 'd.jpg', sortOrder: 0 },
      ],
    });

    const wrapper = mount(DetailPage);
    await flushPromises();

    expect(apiMock.outfits.get).toHaveBeenCalledWith(7);
    const html = wrapper.html();
    expect(html).toContain('周末休闲');
    expect(html).toContain('逛街咖啡');
    expect(html).toContain('已收藏');
  });

  it('clicking 编辑 navigates to outfit-form with id', async () => {
    setOutfitId(7);
    apiMock.outfits.get.mockResolvedValue({ id: 7, name: 'X' });
    const wrapper = mount(DetailPage);
    await flushPromises();

    const editBtn = wrapper.findAll('button').find((b) => b.text().includes('编辑'));
    expect(editBtn).toBeTruthy();
    await editBtn!.trigger('click');
    expect(uniMock.navigateTo).toHaveBeenCalledWith({
      url: '/pages/outfit-form/index?id=7',
    });
  });

  it('clicking 分享 invokes uni.share with detail info', async () => {
    setOutfitId(7);
    apiMock.outfits.get.mockResolvedValue({ id: 7, name: '周末', description: 'desc' });
    const wrapper = mount(DetailPage);
    await flushPromises();

    const shareBtn = wrapper.findAll('button').find((b) => b.text().includes('分享'));
    expect(shareBtn).toBeTruthy();
    await shareBtn!.trigger('click');
    expect(uniMock.share).toHaveBeenCalledTimes(1);
    expect(uniMock.share).toHaveBeenCalledWith(
      expect.objectContaining({ title: '周末' }),
    );
  });

  it('clicking 删除 + confirm triggers remove and navigateBack', async () => {
    setOutfitId(7);
    apiMock.outfits.get.mockResolvedValue({ id: 7, name: 'X' });
    uniMock.showModal.mockImplementationOnce((opts: { success?: (r: { confirm: boolean }) => void }) => {
      opts.success?.({ confirm: true });
    });

    const wrapper = mount(DetailPage);
    await flushPromises();
    const delBtn = wrapper.findAll('button').find((b) => b.text().includes('删除'));
    await delBtn!.trigger('click');
    await flushPromises();

    expect(apiMock.outfits.delete).toHaveBeenCalledWith(7);
    expect(uniMock.navigateBack).toHaveBeenCalled();
  });

  it('does NOT delete when user cancels modal', async () => {
    setOutfitId(7);
    apiMock.outfits.get.mockResolvedValue({ id: 7, name: 'X' });
    uniMock.showModal.mockImplementationOnce((opts: { success?: (r: { confirm: boolean }) => void }) => {
      opts.success?.({ confirm: false });
    });

    const wrapper = mount(DetailPage);
    await flushPromises();
    const delBtn = wrapper.findAll('button').find((b) => b.text().includes('删除'));
    await delBtn!.trigger('click');
    await flushPromises();

    expect(apiMock.outfits.delete).not.toHaveBeenCalled();
  });

  it('get rejection shows empty state', async () => {
    setOutfitId(7);
    apiMock.outfits.get.mockRejectedValueOnce(new Error('network'));
    const wrapper = mount(DetailPage);
    await flushPromises();
    expect(apiMock.outfits.get).toHaveBeenCalledWith(7);
  });
});
