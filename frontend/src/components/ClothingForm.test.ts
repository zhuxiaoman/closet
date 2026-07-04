import { mount } from '@vue/test-utils';
import { describe, it, expect } from 'vitest';
import ClothingForm from './ClothingForm.vue';

// 桩数据：模拟编辑模式下传入的完整 Clothing 对象
const sampleInitial = {
  id: 7,
  name: '黑色卫衣',
  brand: 'GU',
  colorPrimary: '黑',
  colorSecondary: '灰',
  size: 'L',
  season: '秋',
  purchasePrice: 199,
  purchaseDate: '2026-01-15',
  notes: '已穿 5 次',
  mainImageId: 3,
};

describe('ClothingForm', () => {
  it('空表单：提交按钮 disabled 且显示"请输入名称"提示', () => {
    const wrapper = mount(ClothingForm);
    const html = wrapper.html();

    // 校验 1：HTML 中能看到错误提示
    expect(html).toContain('请输入名称');

    // 校验 2：提交按钮带 disabled 属性
    // 用正则定位 class="btn btn-primary" 的那个按钮，确保是提交按钮被禁用
    expect(html).toMatch(/<button[^>]*class="btn btn-primary"[^>]*disabled/);
  });

  it('字段填充后点击保存，emit submit 且 payload 字段正确', async () => {
    const wrapper = mount(ClothingForm);

    // 字段顺序：name, brand, colorPrimary, size, season, purchasePrice, purchaseDate
    const inputs = wrapper.findAll('input');
    await inputs[0].setValue('白T恤');
    await inputs[1].setValue('Uniqlo');
    await inputs[2].setValue('白');
    await inputs[3].setValue('M');
    await inputs[4].setValue('夏');
    await inputs[5].setValue('99');
    await inputs[6].setValue('2026-07-04');

    await wrapper.find('textarea').setValue('夏季常穿');

    // 触发保存按钮（第一个 button）
    await wrapper.findAll('button')[0].trigger('click');

    const events = wrapper.emitted('submit');
    expect(events).toBeTruthy();
    expect(events!.length).toBe(1);

    const payload = events![0][0] as Record<string, unknown>;
    expect(payload.name).toBe('白T恤');
    expect(payload.brand).toBe('Uniqlo');
    expect(payload.colorPrimary).toBe('白');
    expect(payload.size).toBe('M');
    expect(payload.season).toBe('夏');
    expect(payload.purchasePrice).toBe(99);
    expect(payload.purchaseDate).toBe('2026-07-04');
    expect(payload.notes).toBe('夏季常穿');
  });

  it('编辑模式：initial prop 字段预填到表单，name 非空时提交按钮可点击', async () => {
    const wrapper = mount(ClothingForm, {
      props: { initial: sampleInitial },
    });
    const html = wrapper.html();

    // 校验 1：name 非空，提交按钮不应该 disabled
    expect(html).not.toMatch(
      /<button[^>]*class="btn btn-primary"[^>]*disabled/,
    );

    // 校验 2：直接点击保存，emit 出来的 payload 等于 initial
    // （这证明 initial 已正确初始化了 form state，因为 onSubmit 读的是 form.*）
    await wrapper.findAll('button')[0].trigger('click');
    const events = wrapper.emitted('submit');
    expect(events).toBeTruthy();
    const payload = events![0][0] as Record<string, unknown>;
    expect(payload.name).toBe('黑色卫衣');
    expect(payload.brand).toBe('GU');
    expect(payload.colorPrimary).toBe('黑');
    expect(payload.size).toBe('L');
    expect(payload.season).toBe('秋');
    expect(payload.purchasePrice).toBe(199);
    expect(payload.purchaseDate).toBe('2026-01-15');
    expect(payload.notes).toBe('已穿 5 次');
  });

  it('点击取消按钮，emit cancel 事件', async () => {
    const wrapper = mount(ClothingForm, {
      props: { initial: { name: '已填好的T恤' } },
    });
    await wrapper.findAll('button')[1].trigger('click');

    const events = wrapper.emitted('cancel');
    expect(events).toBeTruthy();
    expect(events!.length).toBe(1);
  });

  it('空字符串 / 空白字段在 submit payload 中被剔除', async () => {
    const wrapper = mount(ClothingForm);
    const inputs = wrapper.findAll('input');
    await inputs[0].setValue('极简款');
    // brand 留空，notes 只填空白
    await wrapper.find('textarea').setValue('   ');

    await wrapper.findAll('button')[0].trigger('click');

    const events = wrapper.emitted('submit');
    const payload = events![0][0] as Record<string, unknown>;
    expect(payload.name).toBe('极简款');
    expect('brand' in payload).toBe(false);
    expect('notes' in payload).toBe(false);
    expect('colorPrimary' in payload).toBe(false);
  });
});
