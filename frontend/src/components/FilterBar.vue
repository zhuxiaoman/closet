<template>
  <view class="filter-bar">
    <picker
      :value="seasonIndex"
      :range="seasons"
      @change="onSeason"
    >
      <view class="picker-display">
        <text>{{ seasons[seasonIndex] }}</text>
      </view>
    </picker>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue';

// 通用筛选条件 —— season 字段与后端 clothing 表 CHECK 约束对齐：
//   'spring' | 'summer' | 'fall' | 'winter' | null（null = 全部）
// 其它字段（keyword / categoryIds / tagIds）交给使用方注入，这里只透传。
export interface ClothingFilter {
  season: string | null;
  [k: string]: unknown;
}

const props = defineProps<{ filter: ClothingFilter }>();
const emit = defineEmits<{ change: [f: ClothingFilter] }>();

// picker 第 0 项是占位「全部」，映射回 backend 期望的 null
const seasons = ['全部', 'spring', 'summer', 'fall', 'winter'] as const;

const seasonIndex = computed(() => {
  const i = seasons.findIndex((s) => s === props.filter.season);
  return i >= 0 ? i : 0;
});

function onSeason(e: { detail: { value: number } }) {
  const idx = e.detail.value;
  const nextSeason: string | null = idx === 0 ? null : seasons[idx];
  emit('change', { ...props.filter, season: nextSeason });
}
</script>

<style scoped>
.filter-bar {
  display: flex;
  padding: 16rpx 24rpx;
  background: #fff;
  border-bottom: 1rpx solid #eee;
}
.picker-display {
  display: inline-block;
  padding: 12rpx 24rpx;
  border: 1rpx solid #ddd;
  border-radius: 8rpx;
  font-size: 28rpx;
  color: #333;
}
</style>
