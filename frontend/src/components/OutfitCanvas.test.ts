import { mount } from '@vue/test-utils';
import { describe, it, expect } from 'vitest';
import OutfitCanvas, { type OutfitItem } from './OutfitCanvas.vue';

describe('OutfitCanvas', () => {
  it('renders one tile per item', () => {
    const wrapper = mount(OutfitCanvas, {
      props: {
        items: [
          { id: 1, name: '白色T恤', mainImageKey: 'a.jpg' },
          { id: 2, name: '牛仔裤', mainImageKey: 'b.jpg' },
        ],
      },
    });
    expect(wrapper.findAll('.tile').length).toBe(2);
  });

  it('orders tiles by sortOrder ascending', () => {
    const items: OutfitItem[] = [
      { id: 1, name: '外套', sortOrder: 3 },
      { id: 2, name: 'T恤', sortOrder: 1 },
      { id: 3, name: '裤子', sortOrder: 2 },
    ];
    const wrapper = mount(OutfitCanvas, { props: { items } });
    const tiles = wrapper.findAll('.tile');
    expect(tiles.length).toBe(3);
    expect(tiles[0].text()).toContain('T恤');
    expect(tiles[1].text()).toContain('裤子');
    expect(tiles[2].text()).toContain('外套');
  });

  it('treats missing sortOrder as 0 and orders negatives first', () => {
    const items: OutfitItem[] = [
      { id: 1, name: '无orderA' },
      { id: 2, name: '有orderB', sortOrder: -1 },
      { id: 3, name: '有orderC', sortOrder: 5 },
    ];
    const wrapper = mount(OutfitCanvas, { props: { items } });
    const tiles = wrapper.findAll('.tile');
    expect(tiles[0].text()).toContain('有orderB');
    expect(tiles[1].text()).toContain('无orderA');
    expect(tiles[2].text()).toContain('有orderC');
  });

  it('updates when items prop changes', async () => {
    const wrapper = mount(OutfitCanvas, {
      props: { items: [{ id: 1, name: 'first' }] },
    });
    expect(wrapper.findAll('.tile').length).toBe(1);

    await wrapper.setProps({
      items: [
        { id: 1, name: 'A' },
        { id: 2, name: 'B' },
      ],
    });
    expect(wrapper.findAll('.tile').length).toBe(2);
  });

  it('shows empty hint when items is empty', () => {
    const wrapper = mount(OutfitCanvas, { props: { items: [] } });
    expect(wrapper.findAll('.tile').length).toBe(0);
    expect(wrapper.html()).toContain('还没选衣物');
  });

  it('uses placeholder when item has no mainImageKey', () => {
    const wrapper = mount(OutfitCanvas, {
      props: { items: [{ id: 1, name: 'no-img' }] },
    });
    expect(wrapper.html()).toContain('/static/placeholder.png');
    expect(wrapper.html()).not.toContain('/api/v1/images/');
  });
});
