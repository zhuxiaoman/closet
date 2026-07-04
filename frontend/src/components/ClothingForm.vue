<template>
  <view class="clothing-form">
    <view class="field">
      <text class="label">名称 *</text>
      <input
        v-model="form.name"
        class="input"
        placeholder="例如：白色 T 恤"
      />
      <text v-if="!form.name.trim()" class="hint error">请输入名称</text>
    </view>

    <view class="field">
      <text class="label">品牌</text>
      <input v-model="form.brand" class="input" placeholder="选填" />
    </view>

    <view class="field">
      <text class="label">主色</text>
      <input
        v-model="form.colorPrimary"
        class="input"
        placeholder="选填，如 白/黑/蓝"
      />
    </view>

    <view class="field">
      <text class="label">尺码</text>
      <input v-model="form.size" class="input" placeholder="选填，如 M/L/XL" />
    </view>

    <view class="field">
      <text class="label">季节</text>
      <input
        v-model="form.season"
        class="input"
        placeholder="春 / 夏 / 秋 / 冬 / 四季"
      />
    </view>

    <view class="field">
      <text class="label">购入价格</text>
      <input
        v-model.number="form.purchasePrice"
        class="input"
        type="number"
        placeholder="选填，单位元"
      />
    </view>

    <view class="field">
      <text class="label">购入日期</text>
      <input
        v-model="form.purchaseDate"
        class="input"
        placeholder="选填，如 2026-07-04"
      />
    </view>

    <view class="field">
      <text class="label">备注</text>
      <textarea
        v-model="form.notes"
        class="textarea"
        placeholder="选填"
      />
    </view>

    <view class="actions">
      <button
        type="submit"
        class="btn btn-primary"
        :disabled="!canSubmit"
        @click="onSubmit"
      >
        保存
      </button>
      <button type="button" class="btn btn-secondary" @click="onCancel">
        取消
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive } from 'vue';
// 直接从 schema 拿 Clothing 类型，避免修改 api/index.ts。
// 字段集合来自 openapi-typescript 自动生成的 components['schemas']['Clothing']。
import type { components } from '../api/schema';

type Clothing = components['schemas']['Clothing'];

interface Props {
  initial?: Partial<Clothing>;
}

const props = withDefaults(defineProps<Props>(), {
  initial: () => ({}),
});

const emit = defineEmits<{
  submit: [data: Partial<Clothing>];
  cancel: [];
}>();

// 用 reactive 管理表单状态，新建模式走默认空字符串，
// 编辑模式从 initial prop 复制一份避免外部 prop 被双向改写。
const form = reactive<Partial<Clothing>>({
  name: props.initial?.name ?? '',
  brand: props.initial?.brand ?? '',
  colorPrimary: props.initial?.colorPrimary ?? '',
  size: props.initial?.size ?? '',
  season: props.initial?.season ?? '',
  purchasePrice: props.initial?.purchasePrice,
  purchaseDate: props.initial?.purchaseDate ?? '',
  notes: props.initial?.notes ?? '',
});

const canSubmit = computed(() => form.name.trim().length > 0);

function onSubmit() {
  if (!canSubmit.value) return;
  // 提交时去掉空字符串字段，保持 payload 干净
  const payload: Record<string, unknown> = {
    name: form.name.trim(),
  };
  const stringFields: (keyof Clothing)[] = [
    'brand',
    'colorPrimary',
    'size',
    'season',
    'purchaseDate',
    'notes',
  ];
  for (const key of stringFields) {
    const v = form[key];
    if (typeof v === 'string' && v.trim().length > 0) {
      payload[key] = v.trim();
    }
  }
  if (
    typeof form.purchasePrice === 'number' &&
    !Number.isNaN(form.purchasePrice)
  ) {
    payload.purchasePrice = form.purchasePrice;
  }
  emit('submit', payload as Partial<Clothing>);
}

function onCancel() {
  emit('cancel');
}
</script>

<style scoped>
.clothing-form {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  padding: 24rpx;
  width: 100%;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}
.label {
  font-size: 28rpx;
  color: #333;
}
.input,
.textarea {
  border: 1rpx solid #ddd;
  border-radius: 8rpx;
  padding: 12rpx 16rpx;
  font-size: 28rpx;
  background: #fff;
}
.textarea {
  min-height: 160rpx;
  line-height: 1.4;
}
.hint.error {
  color: #c00;
  font-size: 24rpx;
}
.actions {
  display: flex;
  gap: 16rpx;
  margin-top: 24rpx;
}
.btn {
  flex: 1;
  padding: 20rpx;
  border-radius: 8rpx;
  font-size: 30rpx;
  text-align: center;
}
.btn-primary {
  background: #2979ff;
  color: #fff;
}
.btn-secondary {
  background: #f5f5f5;
  color: #333;
}
.btn[disabled] {
  background: #ccc;
  color: #888;
}
</style>