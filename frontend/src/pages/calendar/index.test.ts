import { mount, flushPromises } from '@vue/test-utils';
import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('../../api', () => ({
  api: {
    calendar: {
      range: vi.fn(),
      create: vi.fn(),
    },
    outfits: {
      list: vi.fn(),
    },
  },
}));

import { api } from '../../api';
import CalendarPage from './index.vue';
import { uniMock } from '../../../vitest.setup';

describe('CalendarPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (api.calendar.range as ReturnType<typeof vi.fn>).mockResolvedValue([]);
    (api.outfits.list as ReturnType<typeof vi.fn>).mockResolvedValue([]);
    (api.calendar.create as ReturnType<typeof vi.fn>).mockResolvedValue({});
  });

  it('renders the "+ 新建" button on mount', async () => {
    const wrapper = mount(CalendarPage);
    await flushPromises();
    const html = wrapper.html();
    expect(html).toContain('+ 新建');
    const buttons = wrapper.findAll('button');
    expect(buttons.length).toBeGreaterThan(0);
    expect(buttons.some((b) => b.text() === '+ 新建')).toBe(true);
  });

  it('renders entry rows from api.calendar.range with outfit names', async () => {
    (api.outfits.list as ReturnType<typeof vi.fn>).mockResolvedValue([
      { id: 1, name: '周末休闲' },
      { id: 2, name: '通勤正装' },
    ]);
    (api.calendar.range as ReturnType<typeof vi.fn>).mockResolvedValue([
      { id: 10, entryDate: '2026-07-10', slot: 'morning', outfitId: 1 },
      { id: 11, entryDate: '2026-07-11', slot: 'afternoon', outfitId: 2 },
    ]);

    const wrapper = mount(CalendarPage);
    await flushPromises();
    const html = wrapper.html();

    expect(html).toContain('2026-07-10');
    expect(html).toContain('2026-07-11');
    expect(html).toContain('周末休闲');
    expect(html).toContain('通勤正装');
    expect(html).toContain('上午');
    expect(html).toContain('下午');
  });

  it('falls back to "?" when outfitId is not found in outfits list', async () => {
    (api.outfits.list as ReturnType<typeof vi.fn>).mockResolvedValue([]);
    (api.calendar.range as ReturnType<typeof vi.fn>).mockResolvedValue([
      { id: 20, entryDate: '2026-07-15', slot: 'evening', outfitId: 999 },
    ]);

    const wrapper = mount(CalendarPage);
    await flushPromises();
    const html = wrapper.html();

    expect(html).toContain('2026-07-15');
    expect(html).toContain('?');
  });

  it('opens the create modal when "+ 新建" is clicked and calls api.calendar.create', async () => {
    (api.outfits.list as ReturnType<typeof vi.fn>).mockResolvedValue([
      { id: 1, name: '周末休闲' },
    ]);

    const wrapper = mount(CalendarPage);
    await flushPromises();

    const buttons = wrapper.findAll('button');
    const fab = buttons.find((b) => b.text() === '+ 新建');
    expect(fab).toBeTruthy();
    await fab!.trigger('click');
    await flushPromises();

    expect(wrapper.html()).toContain('新建日程');

    const saveBtn = wrapper
      .findAll('button')
      .find((b) => b.text().trim() === '保存');
    expect(saveBtn).toBeTruthy();
    await saveBtn!.trigger('click');
    await flushPromises();

    expect(api.calendar.create).toHaveBeenCalledTimes(1);
    const arg = (api.calendar.create as ReturnType<typeof vi.fn>).mock.calls[0][0];
    expect(arg).toMatchObject({ slot: 'morning', outfitId: 1 });
    expect(typeof arg.entryDate).toBe('string');
  });
});

