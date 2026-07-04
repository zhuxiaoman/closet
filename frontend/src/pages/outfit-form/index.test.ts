import { mount, flushPromises } from '@vue/test-utils';
import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('../../api', () => ({
  api: {
    outfits: { get: vi.fn(), create: vi.fn(), update: vi.fn() },
    clothing: { list: vi.fn() },
  },
}));

import { api } from '../../api';
import OutfitFormPage from './index.vue';
import { uniMock } from '../../../vitest.setup';

const apiMock = api as unknown as {
  outfits: { get: ReturnType<typeof vi.fn>; create: ReturnType<typeof vi.fn>; update: ReturnType<typeof vi.fn> };
  clothing: { list: ReturnType<typeof vi.fn> };
};

function setEditId(id: number | null) {
  (globalThis as Record<string, unknown>).__closetOutfitFormEditId = id;
}

describe('OutfitFormPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    setEditId(null);
    apiMock.clothing.list.mockResolvedValue({
      records: [
        { id: 1, name: 'A' },
        { id: 2, name: 'B' },
      ],
      total: 2,
    });
    apiMock.outfits.get.mockResolvedValue({});
    apiMock.outfits.create.mockResolvedValue({ id: 200 });
    apiMock.outfits.update.mockResolvedValue({ id: 1 });
  });

  it('mounts without id, renders blank form, submit calls create', async () => {
    const wrapper = mount(OutfitFormPage);
    await flushPromises();
    expect(apiMock.clothing.list).toHaveBeenCalledTimes(1);
    expect(wrapper.html()).toContain('搭配衣物');
    expect(wrapper.html()).toContain('保存搭配');

    // Open picker, pick first card
    const addBtn = wrapper.findAll('button').find((b) => b.text().includes('+ 添加衣物'))!;
    await addBtn.trigger('click');
    await flushPromises();
    const card = wrapper.findAll('.picker-card').find((c) => c.text().includes('A'))!;
    await card.trigger('click');
    await flushPromises();

    // Set name
    const input = wrapper.find('input');
    await input.setValue('我的搭配');
    await flushPromises();

    // Submit
    const saveBtn = wrapper.findAll('button').find((b) => b.text().includes('保存搭配'))!;
    await saveBtn.trigger('click');
    await flushPromises();

    expect(apiMock.outfits.create).toHaveBeenCalledTimes(1);
    const payload = apiMock.outfits.create.mock.calls[0][0] as { name: string; clothingIds: number[] };
    expect(payload.name).toBe('我的搭配');
    expect(payload.clothingIds).toContain(1);
    expect(uniMock.navigateBack).toHaveBeenCalled();
  });

  it('mounts with id, prefills fields and submit calls update', async () => {
    setEditId(7);
    apiMock.outfits.get.mockResolvedValue({
      id: 7,
      name: '已存在',
      description: 'desc',
      occasion: '周末',
      season: '夏',
      isFavorite: false,
      items: [{ id: 1, name: 'T 恤', sortOrder: 0 }],
    });

    const wrapper = mount(OutfitFormPage);
    await flushPromises();
    expect(apiMock.outfits.get).toHaveBeenCalledWith(7);
    expect((wrapper.vm as { form: { name: string } }).form.name).toBe('已存在');
    expect(wrapper.html()).toContain('保存修改');

    const saveBtn = wrapper.findAll('button').find((b) => b.text().includes('保存修改'))!;
    await saveBtn.trigger('click');
    await flushPromises();

    expect(apiMock.outfits.update).toHaveBeenCalledWith(
      7,
      expect.objectContaining({ name: '已存在', clothingIds: [1] }),
    );
    expect(apiMock.outfits.create).not.toHaveBeenCalled();
  });

  it('save failure shows error toast and does not navigateBack', async () => {
    apiMock.outfits.create.mockRejectedValueOnce(new Error('network'));
    const wrapper = mount(OutfitFormPage);
    await flushPromises();

    const input = wrapper.find('input');
    await input.setValue('X');
    const saveBtn = wrapper.findAll('button').find((b) => b.text().includes('保存搭配'))!;
    await saveBtn.trigger('click');
    await flushPromises();

    expect(apiMock.outfits.create).toHaveBeenCalled();
    expect(uniMock.showToast).toHaveBeenCalledWith(
      expect.objectContaining({ title: '保存失败' }),
    );
    expect(uniMock.navigateBack).not.toHaveBeenCalled();
  });

  it('opening picker and picking clothing adds it to items', async () => {
    const wrapper = mount(OutfitFormPage);
    await flushPromises();

    const addBtn = wrapper.findAll('button').find((b) => b.text().includes('+ 添加衣物'))!;
    await addBtn.trigger('click');
    await flushPromises();
    expect(wrapper.html()).toContain('选择衣物');

    const card = wrapper.findAll('.picker-card').find((c) => c.text().includes('A'))!;
    await card.trigger('click');
    await flushPromises();

    expect((wrapper.vm as { items: unknown[] }).items.length).toBe(1);
  });

  it('removing an item via remove button drops it from items', async () => {
    const wrapper = mount(OutfitFormPage);
    await flushPromises();

    // Pick A
    let addBtn = wrapper.findAll('button').find((b) => b.text().includes('+ 添加衣物'))!;
    await addBtn.trigger('click');
    await flushPromises();
    let card = wrapper.findAll('.picker-card').find((c) => c.text().includes('A'))!;
    await card.trigger('click');
    await flushPromises();

    // Pick B
    addBtn = wrapper.findAll('button').find((b) => b.text().includes('+ 添加衣物'))!;
    await addBtn.trigger('click');
    await flushPromises();
    card = wrapper.findAll('.picker-card').find((c) => c.text().includes('B'))!;
    await card.trigger('click');
    await flushPromises();

    // Both items added
    let items = (wrapper.vm as { items: { id: number; name?: string }[] }).items;
    expect(items.length).toBe(2);
    expect(items[0].name).toBe('A');
    expect(items[1].name).toBe('B');

    // Click remove on A
    const removeBtnA = wrapper.findAll('button').find((b) => b.text().includes('移除') && b.text().includes('A'))!;
    expect(removeBtnA).toBeTruthy();
    await removeBtnA.trigger('click');
    await flushPromises();

    items = (wrapper.vm as { items: { id: number; name?: string }[] }).items;
    expect(items.length).toBe(1);
    expect(items[0].name).toBe('B');
  });

  it('empty name shows error toast and does not call create', async () => {
    const wrapper = mount(OutfitFormPage);
    await flushPromises();
    const saveBtn = wrapper.findAll('button').find((b) => b.text().includes('保存搭配'))!;
    await saveBtn.trigger('click');
    await flushPromises();
    expect(apiMock.outfits.create).not.toHaveBeenCalled();
    expect(uniMock.showToast).toHaveBeenCalledWith(
      expect.objectContaining({ title: '请填写搭配名称' }),
    );
  });
});
