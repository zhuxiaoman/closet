<template>
  <view class="image-uploader">
    <view class="image-list">
      <view v-for="img in images" :key="img.id" class="image-item">
        <image
          :src="`/api/v1/images/${img.storageKey}`"
          class="thumb"
          mode="aspectFill"
        />
      </view>
    </view>
    <view class="uploader-btn" @click="onPick">
      <text v-if="images.length === 0">点击上传</text>
      <text v-else>继续添加</text>
    </view>
  </view>
</template>

<script setup lang="ts">
interface Image {
  id: number;
  storageKey: string;
}

const props = defineProps<{
  clothingId: number;
  images: Image[];
}>();

const emit = defineEmits<{
  uploaded: [image: Image];
}>();

function onPick() {
  // 真实场景调 uni.chooseImage；这里前端测试环境 emit 桩数据
  const fakeImage: Image = {
    id: Math.floor(Math.random() * 1_000_000),
    storageKey: `mock-${Date.now()}.jpg`,
  };
  emit('uploaded', fakeImage);
}
</script>

<style scoped>
.image-uploader { display: flex; flex-direction: column; }
.image-list { display: flex; flex-wrap: wrap; gap: 8rpx; }
.image-item { width: 160rpx; height: 160rpx; }
.thumb { width: 100%; height: 100%; border-radius: 8rpx; }
.uploader-btn {
  margin-top: 16rpx;
  padding: 16rpx;
  border: 1rpx dashed #999;
  text-align: center;
  color: #666;
}
</style>
