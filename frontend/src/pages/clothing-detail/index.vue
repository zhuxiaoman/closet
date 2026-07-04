<template>
  <view class="clothing-detail">
    <view v-if="!detail" class="loading">
      <text>加载中…</text>
    </view>
    <view v-else>
      <view class="header">
        <up-button @click="goEdit">编辑</up-button>
        <up-button @click="onDelete">删除</up-button>
      </view>
      <view class="gallery">
        <image
          v-for="img in detail.images || []"
          :key="img.id"
          :src="`/api/v1/images/${img.storageKey}`"
          class="gallery-img"
          mode="aspectFill"
        />
        <view
          v-if="!detail.images || detail.images.length === 0"
          class="gallery-empty"
        >
          <text>暂无图片</text>
        </view>
      </view>
      <view class="info">
        <view class="row">
          <text class="key">名称</text>
          <text class="val">{{ detail.name || '—' }}</text>
        </view>
        <view class="row">
          <text class="key">品牌</text>
          <text class="val">{{ detail.brand || '—' }}</text>
        </view>
        <view class="row">
          <text class="key">颜色</text>
          <text class="val">{{ colorText }}</text>
        </view>
        <view class="row">
          <text class="key">尺码</text>
          <text class="val">{{ detail.size || '—' }}</text>
        </view>
        <view class="row">
          <text class="key">入手日期</text>
          <text class="val">{{ detail.purchaseDate || '—' }}</text>
        </view>
        <view class="row">
          <text class="key">季节</text>
          <text class="val">{{ detail.season || '—' }}</text>
        </view>
        <view class="row">
          <text class="key">备注</text>
          <text class="val">{{ detail.notes || '—' }}</text>
        </view>
      </view>
      <view class="meta">
        <view class="meta-block">
          <text class="meta-key">分类</text>
          <text class="meta-val">
            {{ (detail.categoryIds || []).join('、') || '—' }}
          </text>
        </view>
        <view class="meta-block">
          <text class="meta-key">标签</text>
          <text class="meta-val">
            {{ (detail.tagIds || []).join('、') || '—' }}
          </text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { api } from '../../api';

interface ClothingImage {
  id: number;
  storageKey: string;
}
interface ClothingDetail {
  id: number;
  name?: string;
  brand?: string;
  colorPrimary?: string;
  colorSecondary?: string;
  size?: string;
  purchaseDate?: string;
  season?: string | null;
  notes?: string;
  categoryIds?: number[];
  tagIds?: number[];
  images?: ClothingImage[];
}

type GlobalWithClosetDetail = {
  __closetDetailId?: number | string | null;
};
type GlobalWithPages = {
  getCurrentPages?: () => Array<{ options?: Record<string, unknown> }>;
};

const detail = ref<ClothingDetail | null>(null);

const colorText = computed(() => {
  if (!detail.value) return '—';
  const parts = [
    detail.value.colorPrimary,
    detail.value.colorSecondary,
  ].filter((x) => x && x.trim().length > 0) as string[];
  return parts.length > 0 ? parts.join(' / ') : '—';
});

function readQueryId(): number | null {
  const g = globalThis as unknown as GlobalWithClosetDetail;
  const fallback = g.__closetDetailId;
  if (fallback !== undefined && fallback !== null && fallback !== '') {
    const n = Number(fallback);
    if (Number.isFinite(n) && n > 0) return n;
  }
  try {
    const pages = (globalThis as unknown as GlobalWithPages).getCurrentPages?.();
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
  try {
    detail.value = (await api.clothing.get(id)) as ClothingDetail;
  } catch {
    detail.value = null;
  }
});

function goEdit() {
  const id = detail.value?.id;
  if (!id) return;
  uni.navigateTo({
    url: '/pages/clothing-form/index?id=' + id,
  });
}

function onDelete() {
  const id = detail.value?.id;
  if (!id) return;
  uni.showModal({
    title: '确认删除',
    content: '删除后无法恢复，确定继续吗？',
    success: async (res) => {
      if (res.confirm) await doDelete(id);
    },
  });
}

async function doDelete(id: number) {
  try {
    await api.clothing.delete(id);
    uni.showToast({ title: '已删除' });
    uni.navigateBack();
  } catch {
    uni.showToast({ title: '删除失败', icon: 'none' });
  }
}
</script>

<style scoped>
.clothing-detail {
  display: flex;
  flex-direction: column;
  padding: 24rpx;
}
.loading {
  padding: 64rpx 0;
  text-align: center;
  color: #999;
}
.header {
  display: flex;
  gap: 16rpx;
  padding-bottom: 24rpx;
}
.gallery {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  padding: 16rpx 0;
}
.gallery-img {
  width: 200rpx;
  height: 200rpx;
  border-radius: 8rpx;
}
.gallery-empty {
  width: 100%;
  text-align: center;
  color: #999;
  padding: 32rpx 0;
}
.info {
  display: flex;
  flex-direction: column;
  border-top: 1rpx solid #eee;
}
.row {
  display: flex;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #eee;
}
.key {
  width: 160rpx;
  color: #666;
  font-size: 26rpx;
}
.val {
  flex: 1;
  font-size: 28rpx;
  color: #333;
}
.meta {
  display: flex;
  gap: 24rpx;
  padding-top: 24rpx;
}
.meta-block {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}
.meta-key {
  font-size: 24rpx;
  color: #999;
}
.meta-val {
  font-size: 28rpx;
  color: #333;
}
</style>
