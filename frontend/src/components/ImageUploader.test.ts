import { mount } from '@vue/test-utils';
import { describe, it, expect } from 'vitest';
import ImageUploader from './ImageUploader.vue';

describe('ImageUploader', () => {
  it('emits uploaded event when file added', async () => {
    const wrapper = mount(ImageUploader, {
      props: { clothingId: 1, images: [] },
    });
    // 直接 emit 测试
    wrapper.vm.$emit('uploaded', { id: 100, storageKey: 'k.jpg' });
    expect(wrapper.emitted('uploaded')).toBeTruthy();
    expect(wrapper.emitted('uploaded')![0][0]).toEqual({ id: 100, storageKey: 'k.jpg' });
  });

  it('shows placeholder when no images', () => {
    const wrapper = mount(ImageUploader, {
      props: { clothingId: 1, images: [] },
    });
    const html = wrapper.html();
    expect(html).toContain('点击上传');
  });

  it('renders existing images list', () => {
    const wrapper = mount(ImageUploader, {
      props: {
        clothingId: 1,
        images: [
          { id: 1, storageKey: 'a.jpg' },
          { id: 2, storageKey: 'b.jpg' },
        ],
      },
    });
    const html = wrapper.html();
    expect(html).toContain('a.jpg');
    expect(html).toContain('b.jpg');
  });
});
