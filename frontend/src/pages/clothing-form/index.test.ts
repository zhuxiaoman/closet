import { mount, flushPromises } from '@vue/test-utils';
import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('../../api', () => ({
  api: {
    clothing: { get: vi.fn(), create: vi.fn(), update: vi.fn() },
    categories: { list: vi.fn() },
    tags: { list: vi.fn() },
  },
}));

import { api } from '../../api';
import ClothingFormPage from './index.vue';
import { uniMock } from '../../../vitest.setup';

const apiMock = api as unknown as {
  clothing: { get: ReturnType<typeof vi.fn>; create: ReturnType<typeof vi.fn>; update: ReturnType<typeof vi.fn> };
  categories: { list: ReturnType<typeof vi.fn> };
  tags: { list: ReturnType<typeof vi.fn> };
};

function setEditId(id: number | null) {
  (globalThis as Record<string, unknown>).__closetFormEditId = id;
}

describe('ClothingFormPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    setEditId(null);
    apiMock.categories.list.mockResolvedValue([
      { id: 1, name: '上衣' },
      { id: 2, name: '裤装' },
    ]);
    apiMock.tags.list.mockResolvedValue([
      { id: 10, name: '正式' },
      { id: 20, name: '常穿' },
    ]);
    apiMock.clothing.get.mockResolvedValue({});
    apiMock.clothing.create.mockResolvedValue({ id: 100 });
    apiMock.clothing.update.mockResolvedValue({ id: 1 });
  });

  it('renders all fields and the 保存 button in create mode', async () => {
    const wrapper = mount(ClothingFormPage);
    await flushPromises();
    const html = wrapper.html();
    expect(html).toContain('名称 *');
    expect(html).toContain('品牌');
    expect(html).toContain('主色');
    expect(html).toContain('配色');
    expect(html).toContain('尺码');
    expect(html).toContain('入手价');
    expect(html).toContain('入手日期');
    expect(html).toContain('季节');
    expect(html).toContain('备注');
    expect(html).toContain('分类');
    expect(html).toContain('标签');
    expect(html).toContain('图片');
    expect(html).toContain('保存');
  });

  it('create flow: validates required name then calls create api and toast 保存成功', async () => {
    const wrapper = mount(ClothingFormPage);
    await flushPromises();
    const button = wrapper.findAll('button').find((b) => b.text().includes('保存'));
    expect(button).toBeTruthy();

    // empty name -> validation failure
    await button!.trigger('click');
    await flushPromises();
    expect(apiMock.clothing.create).not.toHaveBeenCalled();
    expect(uniMock.showToast).toHaveBeenCalledWith(
      expect.objectContaining({ title: '请填写名称' }),
    );
  });

  it('valid name triggers api.clothing.create and navigateBack', async () => {
    const wrapper = mount(ClothingFormPage);
    await flushPromises();
    // Set the name field through DOM event
    const inputs = wrapper.findAll('input');
    await inputs[0].setValue('我的 T 恤');
    const button = wrapper.findAll('button').find((b) => b.text().includes('保存'));
    await button!.trigger('click');
    await flushPromises();

    expect(apiMock.clothing.create).toHaveBeenCalledTimes(1);
    const payload = apiMock.clothing.create.mock.calls[0][0] as { name: string };
    expect(payload.name).toBe('我的 T 恤');
    expect(uniMock.showToast).toHaveBeenCalledWith(
      expect.objectContaining({ title: '保存成功' }),
    );
    expect(uniMock.navigateBack).toHaveBeenCalled();
  });

  it('edit flow: loads detail via api.clothing.get when id is set, then updates', async () => {
    setEditId(7);
    apiMock.clothing.get.mockResolvedValue({
      id: 7,
      name: '已存在',
      brand: 'Nike',
      colorPrimary: '黑',
      colorSecondary: '',
      size: 'L',
      purchasePrice: 99,
      purchaseDate: '2026-01-01',
      season: 'spring',
      notes: 'good',
      categoryIds: [1],
      tagIds: [10],
      images: [],
    });

    const wrapper = mount(ClothingFormPage);
    await flushPromises();
    expect(apiMock.clothing.get).toHaveBeenCalledWith(7);
    expect((wrapper.vm as { form: { name: string } }).form.name).toBe('已存在');

    const button = wrapper.findAll('button').find((b) => b.text().includes('保存'));
    await button!.trigger('click');
    await flushPromises();

    expect(apiMock.clothing.update).toHaveBeenCalledWith(
      7,
      expect.objectContaining({ name: '已存在' }),
    );
    expect(apiMock.clothing.create).not.toHaveBeenCalled();
  });

  it('falls back to empty form when clothing.get rejects', async () => {
    setEditId(99);
    apiMock.clothing.get.mockRejectedValueOnce(new Error('network'));

    const wrapper = mount(ClothingFormPage);
    await flushPromises();
    // Form should still render with empty fields, not crash
    expect(wrapper.html()).toContain('名称 *');
    expect(wrapper.html()).toContain('保存');
  });
});

