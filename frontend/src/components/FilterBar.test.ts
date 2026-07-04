import { mount } from '@vue/test-utils';
import { describe, it, expect } from 'vitest';
import FilterBar from './FilterBar.vue';

describe('FilterBar', () => {
  it('shows "全部" when season is null', () => {
    const wrapper = mount(FilterBar, {
      props: { filter: { season: null } },
    });
    expect(wrapper.html()).toContain('全部');
  });

  it('shows current season text when season is set', () => {
    const wrapper = mount(FilterBar, {
      props: { filter: { season: 'summer' } },
    });
    expect(wrapper.html()).toContain('summer');
  });

  it('updates displayed season after prop change', async () => {
    const wrapper = mount(FilterBar, {
      props: { filter: { season: 'spring' } },
    });
    expect(wrapper.html()).toContain('spring');
    await wrapper.setProps({ filter: { season: 'winter' } });
    expect(wrapper.html()).toContain('winter');
    expect(wrapper.html()).not.toContain('<text>spring</text>');
  });

  it('emits change with null season when picker value is 0', async () => {
    const wrapper = mount(FilterBar, {
      props: { filter: { season: 'summer' } },
    });
    await wrapper
      .findComponent({ name: 'UniPicker' })
      .trigger('change', { detail: { value: 0 } });
    const events = wrapper.emitted('change');
    expect(events).toBeTruthy();
    expect((events![0][0] as { season: string | null }).season).toBeNull();
  });

  it('emits change with corresponding season when picker value is 2 (summer)', async () => {
    const wrapper = mount(FilterBar, {
      props: { filter: { season: 'spring' } },
    });
    await wrapper
      .findComponent({ name: 'UniPicker' })
      .trigger('change', { detail: { value: 2 } });
    const events = wrapper.emitted('change');
    expect(events).toBeTruthy();
    expect((events![0][0] as { season: string | null }).season).toBe('summer');
  });

  it('preserves other filter fields when emitting change', async () => {
    const wrapper = mount(FilterBar, {
      props: { filter: { season: 'spring', keyword: 'T', categoryIds: [1] } },
    });
    await wrapper
      .findComponent({ name: 'UniPicker' })
      .trigger('change', { detail: { value: 1 } });
    const events = wrapper.emitted('change');
    const payload = events![0][0] as {
      season: string;
      keyword?: string;
      categoryIds?: number[];
    };
    expect(payload.season).toBe('spring');
    expect(payload.keyword).toBe('T');
    expect(payload.categoryIds).toEqual([1]);
  });
});
