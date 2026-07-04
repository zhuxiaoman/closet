<template>
  <view class="outfits">
    <view v-if="loading && list.length === 0" class="loading">
      <text>加载中…</text>
    </view>
    <view v-else-if="list.length === 0" class="empty">
      <text>还没有搭配，点击下方按钮创建第一套吧</text>
    </view>
    <view v-else class="grid">
      <view
        v-for="o in list"
        :key="o.id"
        class="card"
        data-test="outfit-card"
        @click="goDetail(o.id)"
      >
        <text class="card-title">{{ o.name }}</text>
        <text v-if="o.description" class="card-desc">{{ o.description }}</text>
        <text class="card-meta">包含 {{ itemCount(o) }} 件衣物</text>
      </view>
    </view>
    <view class="footer">
      <up-button @click="goForm()">+ 新建搭配</up-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { api } from '../../api';

interface OutfitRecord {
  id: number;
  name: string;
  description?: string;
  itemCount?: number;
  items?: unknown[];
}

const list = ref<OutfitRecord[]>([]);
const loading = ref(false);

async function load() {
  loading.value = true;
  try {
    const data = (await api.outfits.list()) as { records?: OutfitRecord[] };
    list.value = data.records ?? [];
  } catch {
    list.value = [];
  } finally {
    loading.value = false;
  }
}

onMounted(load);

function itemCount(o: OutfitRecord): number {
  if (typeof o.itemCount === 'number') return o.itemCount;
  if (Array.isArray(o.items)) return o.items.length;
  return 0;
}

function goDetail(id: number) {
  uni.navigateTo({ url: '/pages/outfit-detail/index?id=' + id });
}

function goForm() {
  uni.navigateTo({ url: '/pages/outfit-form/index' });
}
</script>

<style scoped>
.outfits {
  display: flex;
  flex-direction: column;
  padding: 16rpx;
  min-height: 100vh;
  background: #fafafa;
}
.loading,
.empty {
  padding: 96rpx 0;
  text-align: center;
  color: #999;
  font-size: 28rpx;
}
.grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  padding: 16rpx 0;
}
.card {
  display: flex;
  flex-direction: column;
  width: calc(50% - 8rpx);
  padding: 16rpx;
  background: #fff;
  border-radius: 8rpx;
  box-sizing: border-box;
}
.card-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 6rpx;
}
.card-desc {
  font-size: 24rpx;
  color: #666;
  margin-bottom: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.card-meta {
  font-size: 22rpx;
  color: #999;
}
.footer {
  padding: 24rpx 0 32rpx;
}
</style>
