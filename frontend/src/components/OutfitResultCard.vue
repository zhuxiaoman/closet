<template>
  <view class="result-card">
    <view class="head">
      <text class="index">搭配 {{ index }}</text>
    </view>
    <view class="items">
      <text v-for="(item, i) in items" :key="i" class="item">{{ item }}</text>
    </view>
    <view class="actions">
      <button
        data-test="like"
        class="action like"
        @click="emit('like')"
      >
        <Icon name="thumbs-up" :size="20" color="#ffffff" />
        <text class="action-text">喜欢</text>
      </button>
      <button
        data-test="dislike"
        class="action"
        @click="emit('dislike')"
      >
        <Icon name="thumbs-down" :size="20" />
        <text class="action-text">不感兴趣</text>
      </button>
      <button
        data-test="save"
        class="action"
        @click="emit('save')"
      >
        <Icon name="bookmark" :size="20" />
        <text class="action-text">收藏</text>
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import Icon from '@/components/Icon.vue';

defineProps<{
  items: string[];
  index: number;
  generationId?: number;
}>();

const emit = defineEmits<{
  like: [];
  dislike: [];
  save: [];
}>();
</script>

<style lang="scss" scoped>
.result-card {
  background: var(--surface, var(--bg-card, #f9f0ee));
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 12rpx var(--shadow, rgba(184, 128, 138, 0.12));
}
.head {
  margin-bottom: 12rpx;
}
.index {
  font-size: 28rpx;
  font-weight: 600;
  color: var(--text-primary, #4a3a3a);
}
.items {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
  margin-bottom: 16rpx;
}
.item {
  padding: 4rpx 16rpx;
  background: var(--accent-soft, #fdeef0);
  color: var(--accent, #d49aa5);
  border-radius: 24rpx;
  font-size: 24rpx;
}
.actions {
  display: flex;
  gap: 16rpx;
}
.action {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 8rpx 24rpx;
  border-radius: 24rpx;
  background: var(--bg-elevated, #fafafa);
  color: var(--text-primary, #4a3a3a);
  font-size: 24rpx;
}
.action.like {
  background: var(--accent, #d49aa5);
  color: #ffffff;
}
.action-text {
  font-size: 24rpx;
}
</style>