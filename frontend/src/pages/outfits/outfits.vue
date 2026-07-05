<template>
  <view class="page">
    <text class="title">搭配</text>
    <view v-if="loading" class="loading"><text>加载中…</text></view>
    <view v-else-if="outfits.length === 0" class="empty">
      <text class="empty-text">还没有搭配</text>
      <button class="primary" @click="goAdd">创建第一套</button>
    </view>
    <view v-else class="list">
      <view v-for="o in outfits" :key="o.id" class="row" @click="openDetail(o)">
        <text class="o-title">{{ o.title || '搭配 ' + o.id }}</text>
        <text class="o-sub">{{ (o.clothingIds || []).length }} 件单品</text>
        <i-lucide-chevron-right />
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { api } from '../api'

const outfits = ref<any[]>([])
const loading = ref(true)

onShow(async () => {
  loading.value = true
  try {
    const res = await api.outfit.list()
    outfits.value = (res as any)?.data ?? (res as any) ?? []
  } finally {
    loading.value = false
  }
})

function openDetail(o: any) { uni.navigateTo({ url: `/pages/outfit-detail/index?id=${o.id}` }) }
function goAdd() { uni.navigateTo({ url: '/pages/outfit-form/index' }) }
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: var(--bg); padding: 32rpx 24rpx; }
.title { font-size: 48rpx; font-weight: 700; color: var(--text-primary); display: block; margin-bottom: 24rpx; }
.loading, .empty { display: flex; flex-direction: column; align-items: center; padding: 80rpx 0; gap: 16rpx; }
.empty-text { color: var(--text-secondary); font-size: 28rpx; }
.primary { background: var(--accent); color: white; border-radius: 32rpx; padding: 20rpx 40rpx; margin-top: 16rpx; }
.list { display: flex; flex-direction: column; gap: 12rpx; }
.row { background: var(--surface); padding: 24rpx; border-radius: 16rpx; display: flex; align-items: center; gap: 12rpx; }
.o-title { flex: 1; font-size: 28rpx; font-weight: 600; color: var(--text-primary); }
.o-sub { font-size: 24rpx; color: var(--text-secondary); margin-right: 12rpx; }
</style>