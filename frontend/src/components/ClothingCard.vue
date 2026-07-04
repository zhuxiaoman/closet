<template>
  <view class="clothing-card">
    <image
      :src="imgSrc"
      class="img"
      mode="aspectFill"
    />
    <view class="name">{{ clothing.name }}</view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue';

interface Clothing {
  id: number;
  name: string;
  mainImageId?: number;
  mainImageKey?: string;
}

const props = defineProps<{ clothing: Clothing }>();

const imgSrc = computed(() => {
  if (props.clothing.mainImageKey) {
    return `/api/v1/images/${props.clothing.mainImageKey}`;
  }
  return '/static/placeholder.png';
});
</script>

<style scoped>
.clothing-card { display: flex; flex-direction: column; }
.img { width: 160rpx; height: 160rpx; border-radius: 8rpx; }
.name { font-size: 28rpx; margin-top: 8rpx; }
</style>
