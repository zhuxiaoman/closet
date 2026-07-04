import { mount, flushPromises } from '@vue/test-utils';
import { describe, it, expect, vi, beforeEach } from 'vitest';

// mock api 模块：用 vi.fn() 替换 fetch 链路
vi.mock('../../api', () => ({
  api: {
    categories: {
      list: vi.fn(),
      create: vi.fn(),
      update: vi.fn(),
      delete: vi.fn(),
    },
    tags: {
      list: vi.fn(),
      create: vi.fn(),
      update: vi.fn(),
      delete: vi.fn(),
    },
  },
}));

import { api } from '../../api';
import SettingsPage from './index.vue';
import { uniMock } from '../../../vitest.setup';

describe('SettingsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (api.categories.list as ReturnType<typeof vi.fn>).mockResolvedValue([]);
    (api.categories.create as ReturnType<typeof vi.fn>).mockResolvedValue({});
    (api.categories.delete as ReturnType<typeof vi.fn>).mockResolvedValue({});
    (api.tags.list as ReturnType<typeof vi.fn>).mockResolvedValue([]);
    (api.tags.create as ReturnType<typeof vi.fn>).mockResolvedValue({});
    (api.tags.delete as ReturnType<typeof vi.fn>).mockResolvedValue({});
  });

  it('renders section titles and action buttons before data resolves', () => {
    (api.categories.list as ReturnType<typeof vi.fn>).mockImplementation(
      () => new Promise(() => {}),
    );
    (api.tags.list as ReturnType<typeof vi.fn>).mockImplementation(
      () => new Promise(() => {}),
    );
    const wrapper = mount(SettingsPage);
    const html = wrapper.html();
    expect(html).toContain('分类管理');
    expect(html).toContain('标签管理');
    expect(html).toContain('数据导出');
    expect(html).toContain('+ 新增分类');
    expect(html).toContain('+ 新增标签');
    // 空状态
    expect(html).toContain('暂无分类');
    expect(html).toContain('暂无标签');
  });

  it('displays category and tag names returned from the api', async () => {
    (api.categories.list as ReturnType<typeof vi.fn>).mockResolvedValue([
      { id: 1, name: '上装' },
      { id: 2, name: '下装' },
    ]);
    (api.tags.list as ReturnType<typeof vi.fn>).mockResolvedValue([
      { id: 10, name: '通勤' },
      { id: 11, name: '休闲' },
    ]);
    const wrapper = mount(SettingsPage);
    await flushPromises();
    const html = wrapper.html();
    expect(html).toContain('上装');
    expect(html).toContain('下装');
    expect(html).toContain('通勤');
    expect(html).toContain('休闲');
  });

  it('clicking "+ 新增分类" calls uni.showModal and creates a category on confirm', async () => {
    (uniMock.showModal as ReturnType<typeof vi.fn>).mockResolvedValue({
      confirm: true,
      content: '配件',
    });
    const wrapper = mount(SettingsPage);
    await flushPromises();
    const buttons = wrapper.findAll('button');
    // 第一个 button 是 "删除"（空列表时不渲染，但 list-row 不存在时只有 + 新增分类/+ 新增标签/数据导出）
    // 我们按文案查找 + 新增分类
    const addCategoryBtn = buttons.find((b) => b.text().includes('+ 新增分类'));
    expect(addCategoryBtn).toBeTruthy();
    await addCategoryBtn!.trigger('click');
    expect(uniMock.showModal).toHaveBeenCalledTimes(1);
    expect(api.categories.create).toHaveBeenCalledWith({ name: '配件' });
  });

  it('clicking "数据导出" shows a toast (placeholder behavior)', async () => {
    const wrapper = mount(SettingsPage);
    await flushPromises();
    const buttons = wrapper.findAll('button');
    const exportBtn = buttons.find((b) => b.text().includes('数据导出'));
    expect(exportBtn).toBeTruthy();
    await exportBtn!.trigger('click');
    expect(uniMock.showToast).toHaveBeenCalledTimes(1);
    expect(uniMock.showToast).toHaveBeenCalledWith(
      expect.objectContaining({ title: '导出功能开发中' }),
    );
  });

  it('clicking a category 删除 button calls api.categories.delete and reloads', async () => {
    (api.categories.list as ReturnType<typeof vi.fn>).mockResolvedValueOnce([
      { id: 99, name: '外套' },
    ]);
    const wrapper = mount(SettingsPage);
    await flushPromises();
    const buttons = wrapper.findAll('button');
    const deleteBtn = buttons.find((b) => b.text().includes('删除'));
    expect(deleteBtn).toBeTruthy();
    await deleteBtn!.trigger('click');
    expect(api.categories.delete).toHaveBeenCalledWith(99);
  });
});

