import { mount, flushPromises } from '@vue/test-utils';
import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('../../api', () => ({
  api: {
    clothing: { get: vi.fn(), delete: vi.fn() },
  },
}));

import { api } from '../../api';
import ClothingDetailPage from './index.vue';
import { uniMock } from '../../../vitest.setup';

const apiMock = api as unknown as {
  clothing: { get: ReturnType<typeof vi.fn>; delete: ReturnType<typeof vi.fn> };
};

function setDetailId(id: number | null) {
  (globalThis as Record<string, unknown>).__closetDetailId = id;
}

describe('ClothingDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    setDetailId(null);
  });

  it('fetches detail via api.clothing.get and renders key fields', async () => {
    setDetailId(7);
    apiMock.clothing.get.mockResolvedValue({
      id: 7,
      name: '白色 T 恤',
      brand: 'Uniqlo',
      colorPrimary: '白色',
      colorSecondary: '',
      size: 'M',
      purchaseDate: '2026-05-01',
      season: 'summer',
      notes: '舒服',
      categoryIds: [1, 2],
      tagIds: [10],
      images: [{ id: 100, storageKey: 'a.jpg' }],
    });

    const wrapper = mount(ClothingDetailPage);
    await flushPromises();

    expect(apiMock.clothing.get).toHaveBeenCalledWith(7);
    const html = wrapper.html();
    expect(html).toContain('白色 T 恤');
    expect(html).toContain('Uniqlo');
    expect(html).toContain('白色');
    expect(html).toContain('M');
    expect(html).toContain('summer');
    expect(html).toContain('舒服');
    expect(html).toContain('编辑');
    expect(html).toContain('删除');
  });

  it('clicking 编辑 navigates to clothing-form with id query', async () => {
    setDetailId(12);
    apiMock.clothing.get.mockResolvedValue({ id: 12, name: '牛仔裤' });

    const wrapper = mount(ClothingDetailPage);
    await flushPromises();
    const editBtn = wrapper.findAll('button').find((b) => b.text().includes('编辑'));
    expect(editBtn).toBeTruthy();
    await editBtn!.trigger('click');
    expect(uniMock.navigateTo).toHaveBeenCalledWith({
      url: '/pages/clothing-form/index?id=12',
    });
  });

  it('clicking 删除 shows a confirm modal; on confirm calls delete and navigates back', async () => {
    setDetailId(5);
    apiMock.clothing.get.mockResolvedValue({ id: 5, name: 'X' });
    // simulate the user confirming the modal: invoke the success callback with confirm:true
    uniMock.showModal.mockImplementationOnce((opts: { success?: (r: { confirm: boolean }) => void }) => {
      opts.success?.({ confirm: true });
    });

    const wrapper = mount(ClothingDetailPage);
    await flushPromises();
    const delBtn = wrapper.findAll('button').find((b) => b.text().includes('删除'));
    expect(delBtn).toBeTruthy();
    await delBtn!.trigger('click');
    await flushPromises();

    expect(uniMock.showModal).toHaveBeenCalledTimes(1);
    expect(apiMock.clothing.delete).toHaveBeenCalledWith(5);
    expect(uniMock.navigateBack).toHaveBeenCalled();
  });

  it('does NOT delete when user cancels the modal', async () => {
    setDetailId(6);
    apiMock.clothing.get.mockResolvedValue({ id: 6, name: 'Y' });
    uniMock.showModal.mockImplementationOnce((opts: { success?: (r: { confirm: boolean }) => void }) => {
      opts.success?.({ confirm: false });
    });

    const wrapper = mount(ClothingDetailPage);
    await flushPromises();
    const delBtn = wrapper.findAll('button').find((b) => b.text().includes('删除'));
    await delBtn!.trigger('click');
    await flushPromises();

    expect(apiMock.clothing.delete).not.toHaveBeenCalled();
  });

  it('keeps null detail when api.clothing.get rejects', async () => {
    setDetailId(9);
    apiMock.clothing.get.mockRejectedValueOnce(new Error('network'));
    const wrapper = mount(ClothingDetailPage);
    await flushPromises();
    expect(apiMock.clothing.get).toHaveBeenCalledWith(9);
  });
});
