<template>
  <view class="page">
    <text class="title">衣橱</text>
    <view v-if="loading" class="loading"><text>加载中…</text></view>
    <view v-else-if="clothes.length === 0" class="empty">
      <text class="empty-text">衣橱空空如也</text>
      <button class="primary" @click="goAdd">添加第一件衣物</button>
    </view>
    <view v-else class="grid">
      <view v-for="c in clothes" :key="c.id" class="cell" @click="openDetail(c)">
        <image :src="c.imageUrl" class="img" mode="aspectFill" />
        <text class="name">{{ c.name }}</text>
        <text class="cat">{{ catLabel(c.category) }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { api } from '../api'

const clothes = ref<any[]>([])
const loading = ref(true)

onShow(async () => {
  loading.value = true
  try {
    const res = await api.clothing.list()
    clothes.value = (res as any)?.data ?? (res as any) ?? []
  } finally {
    loading.value = false
  }
})

const catLabels: Record<string, string> = {
  top: '上装', bottom: '下装', outer: '外套', dress: '连衣裙',
  shoes: '鞋履', bag: '包包', accessory: '配饰', underwear: '内衣',
}
function catLabel(k: string) { return catLabels[k] ?? k }

function openDetail(c: any) { uni.navigateTo({ url: `/pages/clothing-detail/index?id=${c.id}` }) }
function goAdd() { uni.navigateTo({ url: '/pages/clothing-form/index' }) }
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: var(--bg); padding: 32rpx 24rpx; }
.title { font-size: 48rpx; font-weight: 700; color: var(--text-primary); display: block; margin-bottom: 24rpx; }
.loading, .empty { display: flex; flex-direction: column; align-items: center; padding: 80rpx 0; gap: 16rpx; }
.empty-text { color: var(--text-secondary); font-size: 28rpx; }
.primary { background: var(--accent); color: white; border-radius: 32rpx; padding: 20rpx 40rpx; margin-top: 16rpx; }
.grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16rpx; }
.cell { background: var(--surface); border-radius: 16rpx; overflow: hidden; }
.img { width: 100%; height: 220rpx; display: block; }
.name { font-size: 24rpx; padding: 8rpx 12rpx 0; display: block; color: var(--text-primary); }
.cat { font-size: 22rpx; padding: 0 12rpx 8rpx; display: block; color: var(--text-secondary); }
</style>