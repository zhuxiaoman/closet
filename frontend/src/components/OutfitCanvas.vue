<template>
  <view class="canvas">
    <view v-for="item in sorted" :key="item.id" class="tile">
      <image
        :src="imgSrc(item)"
        class="tile-img"
        mode="aspectFill"
      />
      <text class="tile-name">{{ item.name ?? '' }}</text>
    </view>
    <view v-if="sorted.length === 0" class="empty">
      <text class="empty-text">还没选衣物，点 + 添加</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue';

export interface OutfitItem {
  id: number;
  name?: string;
  mainImageKey?: string;
  sortOrder?: number;
}

const props = defineProps<{ items: OutfitItem[] }>();

// 按 sortOrder 升序，缺失视为 0（放在中间档位）。
// 注意：原数组 props.items 不变更，避免外部引用被原地排序。
const sorted = computed<OutfitItem[]>(() =>
  [...props.items].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0)),
);

function imgSrc(item: OutfitItem): string {
  return item.mainImageKey
    ? `/api/v1/images/${item.mainImageKey}`
    : '/static/placeholder.png';
}
</script>

<style scoped>
.canvas {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  padding: 16rpx;
}
.tile {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 200rpx;
}
.tile-img {
  width: 200rpx;
  height: 200rpx;
  border-radius: 8rpx;
  background: #f5f5f5;
}
.tile-name {
  font-size: 26rpx;
  margin-top: 8rpx;
  text-align: center;
  color: #333;
}
.empty {
  width: 100%;
  padding: 48rpx 24rpx;
  text-align: center;
}
.empty-text {
  font-size: 28rpx;
  color: #999;
}
</style>
