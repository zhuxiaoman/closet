<template>
  <view class="clothing-list">
    <view v-if="error" class="state error">
      <text class="state-text">{{ error }}</text>
    </view>
    <view v-else-if="loading" class="state loading">
      <text class="state-text">加载中…</text>
    </view>
    <view v-else-if="items.length === 0" class="state empty">
      <text class="state-text">暂无衣物，点击 + 添加第一件</text>
    </view>
    <view v-else class="list">
      <view
        v-for="item in items"
        :key="item.id"
        class="list-item"
      >
        <UniClothingCard
          :clothing="item"
          @click="onItemClick(item)"
        />
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import type { Clothing } from '../api';
import UniClothingCard from './ClothingCard.vue';

withDefaults(
  defineProps<{
    items: Clothing[];
    loading?: boolean;
    error?: string | null;
  }>(),
  {
    loading: false,
    error: null,
  },
);

const emit = defineEmits<{
  'item-click': [item: Clothing];
}>();

function onItemClick(item: Clothing) {
  emit('item-click', item);
}
</script>

<style scoped>
.clothing-list { display: flex; flex-direction: column; width: 100%; }
.state { padding: 48rpx 24rpx; text-align: center; }
.state.error .state-text { color: #c00; font-size: 28rpx; }
.state.loading .state-text { color: #666; font-size: 28rpx; }
.state.empty .state-text { color: #999; font-size: 28rpx; }
.list { display: flex; flex-direction: column; gap: 16rpx; }
.list-item { width: 100%; }
</style>
