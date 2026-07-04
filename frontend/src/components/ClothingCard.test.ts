import { mount } from '@vue/test-utils';
import { describe, it, expect } from 'vitest';
import ClothingCard from './ClothingCard.vue';

describe('ClothingCard', () => {
  it('renders name and main image', () => {
    const wrapper = mount(ClothingCard, {
      props: {
        clothing: { id: 1, name: '白 T', mainImageId: 10, mainImageKey: 'k.jpg' },
      },
    });
    const html = wrapper.html();
    expect(html).toContain('白 T');
    expect(html).toContain('/api/v1/images/k.jpg');
  });

  it('falls back to placeholder when no image', () => {
    const wrapper = mount(ClothingCard, {
      props: { clothing: { id: 1, name: 'x' } },
    });
    const html = wrapper.html();
    expect(html).toContain('placeholder');
  });
});
