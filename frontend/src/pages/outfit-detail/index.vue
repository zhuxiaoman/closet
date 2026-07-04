<template>
  <view class="outfit-detail">
    <view v-if="loading" class="loading">
      <text>加载中…</text>
    </view>
    <view v-else-if="!detail" class="empty">
      <text>搭配不存在或已被删除</text>
    </view>
    <view v-else class="content">
      <view class="header">
        <text class="title">{{ detail.name }}</text>
        <text v-if="detail.isFavorite" class="favorite">★ 已收藏</text>
      </view>
      <view v-if="detail.description" class="desc">
        <text>{{ detail.description }}</text>
      </view>
      <view class="meta">
        <view v-if="detail.occasion" class="meta-item">
          <text class="meta-label">场合：</text>
          <text>{{ detail.occasion }}</text>
        </view>
        <view v-if="detail.season" class="meta-item">
          <text class="meta-label">季节：</text>
          <text>{{ detail.season }}</text>
        </view>
      </view>

      <view class="section">
        <text class="section-title">搭配预览</text>
        <OutfitCanvas :items="detail.items ?? []" />
      </view>

      <view class="actions">
        <up-button @click="goEdit">编辑</up-button>
        <up-button @click="onShare">分享</up-button>
        <up-button @click="onDelete">删除</up-button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { api } from '../../api';
import OutfitCanvas, { type OutfitItem } from '../../components/OutfitCanvas.vue';

interface OutfitDetail {
  id: number;
  name: string;
  description?: string;
  occasion?: string;
  season?: string;
  isFavorite?: boolean;
  items?: OutfitItem[];
}

const detail = ref<OutfitDetail | null>(null);
const loading = ref(false);

function readQueryId(): number | null {
  const g = globalThis as unknown as { __closetOutfitDetailId?: number | string | null };
  const fallback = g.__closetOutfitDetailId;
  if (fallback !== undefined && fallback !== null && fallback !== '') {
    const n = Number(fallback);
    if (Number.isFinite(n) && n > 0) return n;
  }
  try {
    const pages = (globalThis as unknown as { getCurrentPages?: () => Array<{ options?: Record<string, unknown> }> }).getCurrentPages?.();
    const opt = pages?.at(-1)?.options ?? {};
    const raw = opt.id;
    if (raw === undefined || raw === null || raw === '') return null;
    const n = Number(raw);
    return Number.isFinite(n) && n > 0 ? n : null;
  } catch {
    return null;
  }
}

onMounted(async () => {
  const id = readQueryId();
  if (id === null) return;
  loading.value = true;
  try {
    detail.value = (await api.outfits.get(id)) as OutfitDetail;
  } catch {
    detail.value = null;
  } finally {
    loading.value = false;
  }
});

function goEdit() {
  if (!detail.value) return;
  uni.navigateTo({ url: '/pages/outfit-form/index?id=' + detail.value.id });
}

function onShare() {
  if (!detail.value) return;
  uni.share({
    provider: 'weixin',
    type: 0,
    title: detail.value.name,
    summary: detail.value.description ?? '',
    href: '',
    imageUrl: '',
    success: () => {
      uni.showToast({ title: '已分享' });
    },
    fail: () => {
      uni.showToast({ title: '分享失败', icon: 'none' });
    },
  });
}

function onDelete() {
  if (!detail.value) return;
  uni.showModal({
    title: '删除搭配',
    content: '确定要删除这套搭配吗？',
    success: async (res) => {
      if (!res.confirm) return;
      try {
        await api.outfits.delete(detail.value!.id);
        uni.showToast({ title: '已删除' });
        uni.navigateBack();
      } catch {
        uni.showToast({ title: '删除失败', icon: 'none' });
      }
    },
  });
}
</script>

<style scoped>
.outfit-detail {
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
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx;
  background: #fff;
  border-radius: 8rpx;
}
.title {
  font-size: 36rpx;
  font-weight: 600;
  color: #333;
}
.favorite {
  font-size: 24rpx;
  color: #ff9900;
}
.desc {
  margin-top: 16rpx;
  padding: 16rpx;
  background: #fff;
  border-radius: 8rpx;
  font-size: 28rpx;
  color: #666;
  line-height: 1.6;
}
.meta {
  margin-top: 16rpx;
  padding: 16rpx;
  background: #fff;
  border-radius: 8rpx;
}
.meta-item {
  display: flex;
  font-size: 26rpx;
  color: #666;
  margin-bottom: 6rpx;
}
.meta-item:last-child {
  margin-bottom: 0;
}
.meta-label {
  color: #999;
  min-width: 96rpx;
}
.section {
  margin-top: 16rpx;
  padding: 16rpx;
  background: #fff;
  border-radius: 8rpx;
}
.section-title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 8rpx;
}
.actions {
  display: flex;
  gap: 16rpx;
  margin-top: 24rpx;
}
</style>
