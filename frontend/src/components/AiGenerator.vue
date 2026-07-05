<template>
  <view class="ai-generator">
    <view class="seed-row">
      <text class="label">起点单品</text>
      <view v-if="seedIds.length === 0" class="empty-seed">
        <text class="empty-seed-text">还没有选择起点单品</text>
      </view>
      <view v-else class="chips">
        <view v-for="id in seedIds" :key="id" class="chip">{{ id }}</view>
      </view>
    </view>

    <picker
      mode="selector"
      :range="occasionLabels"
      :value="occasionIdx"
      @change="onOccasion"
    >
      <view class="picker">
        <text class="picker-text">场景：{{ occasionLabels[occasionIdx] }}</text>
      </view>
    </picker>

    <picker
      mode="selector"
      :range="seasonLabels"
      :value="seasonIdx"
      @change="onSeason"
    >
      <view class="picker">
        <text class="picker-text">季节：{{ seasonLabels[seasonIdx] }}</text>
      </view>
    </picker>

    <button
      class="primary"
      :disabled="generating || seedIds.length === 0"
      data-test="ai-go"
      @click="emitGenerate"
    >
      <text v-if="generating">生成中…</text>
      <text v-else>AI 生成 5 套搭配</text>
    </button>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';

const props = defineProps<{ seedIds: number[]; generating: boolean }>();

const emit = defineEmits<{
  generate: [
    req: { seedIds: number[]; occasion: string; season: string },
  ];
}>();

const occasions = ['casual', 'work', 'date', 'sport', 'home'] as const;
const occasionLabels = ['日常', '通勤', '约会', '运动', '居家'] as const;
const seasons = ['all', 'spring', 'summer', 'autumn', 'winter'] as const;
const seasonLabels = ['全年', '春', '夏', '秋', '冬'] as const;

const occasionIdx = ref(0);
const seasonIdx = ref(0);

function onOccasion(e: { detail: { value: number } }) {
  occasionIdx.value = e.detail.value;
}

function onSeason(e: { detail: { value: number } }) {
  seasonIdx.value = e.detail.value;
}

function emitGenerate() {
  emit('generate', {
    seedIds: props.seedIds,
    occasion: occasions[occasionIdx.value],
    season: seasons[seasonIdx.value],
  });
}
</script>

<style lang="scss" scoped>
.ai-generator {
  padding: 24rpx;
  background: var(--surface, var(--bg-card, #f9f0ee));
  border-radius: 16rpx;
}
.seed-row {
  margin-bottom: 16rpx;
}
.label {
  font-size: 26rpx;
  color: var(--text-secondary, #8a6d70);
}
.empty-seed {
  margin-top: 8rpx;
}
.empty-seed-text {
  font-size: 24rpx;
  color: var(--text-secondary, #8a6d70);
}
.chips {
  display: flex;
  gap: 12rpx;
  margin-top: 8rpx;
  flex-wrap: wrap;
}
.chip {
  padding: 4rpx 16rpx;
  background: var(--accent-soft, #fdeef0);
  color: var(--accent, #d49aa5);
  border-radius: 24rpx;
  font-size: 24rpx;
}
.picker {
  padding: 16rpx;
  background: var(--bg-elevated, #fafafa);
  border-radius: 12rpx;
  margin-bottom: 12rpx;
}
.picker-text {
  font-size: 28rpx;
  color: var(--text-primary, #4a3a3a);
}
.primary {
  margin-top: 16rpx;
  background: var(--accent, #d49aa5);
  color: #ffffff;
  border-radius: 32rpx;
  padding: 20rpx 0;
  font-size: 28rpx;
}
.primary[disabled] {
  opacity: 0.6;
}
</style>
