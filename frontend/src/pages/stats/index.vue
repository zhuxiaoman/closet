<template>
  <view class="stats-page">
    <up-card>
      <view class="overview-card">
        <view class="overview-item">
          <text class="overview-num">{{ overview.totalClothing }}</text>
          <text class="overview-label">衣物</text>
        </view>
        <view class="overview-item">
          <text class="overview-num">{{ overview.totalOutfits }}</text>
          <text class="overview-label">搭配</text>
        </view>
        <view class="overview-item">
          <text class="overview-num">{{ overview.monthWears }}</text>
          <text class="overview-label">本月穿</text>
        </view>
      </view>
    </up-card>

    <up-card>
      <view class="section">
        <text class="section-title">最常穿 Top 10</text>
        <view
          v-for="item in mostWorn"
          :key="item.clothingId"
          class="list-item"
          @click="goDetail(item.clothingId)"
        >
          <text class="list-name">{{ item.name }}</text>
          <text class="list-meta">{{ item.wearCount }} 次</text>
        </view>
        <text v-if="!mostWorn.length" class="empty">暂无数据</text>
      </view>
    </up-card>

    <up-card>
      <view class="section">
        <text class="section-title">最少穿（90 天未穿）</text>
        <view
          v-for="item in leastWorn"
          :key="item.clothingId"
          class="list-item"
        >
          <text class="list-name">{{ item.name }}</text>
          <text class="list-meta">{{ item.daysSinceLastWear }} 天前穿</text>
        </view>
        <text v-if="!leastWorn.length" class="empty">暂无数据</text>
      </view>
    </up-card>

    <up-card>
      <view class="section">
        <text class="section-tip">
          统计数据每日凌晨更新。最常穿 / 最少穿帮助你发现闲置衣物。
        </text>
      </view>
    </up-card>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { api } from '../../api';

interface StatsOverview {
  totalClothing: number;
  totalOutfits: number;
  monthWears: number;
}

interface MostWornItem {
  clothingId: number;
  name: string;
  wearCount: number;
}

interface LeastWornItem {
  clothingId: number;
  name: string;
  daysSinceLastWear: number;
}

const overview = ref<StatsOverview>({
  totalClothing: 0,
  totalOutfits: 0,
  monthWears: 0,
});
const mostWorn = ref<MostWornItem[]>([]);
const leastWorn = ref<LeastWornItem[]>([]);

onMounted(async () => {
  try {
    const [ov, mw, lw] = await Promise.all([
      api.stats.overview(),
      api.stats.mostWorn(10),
      api.stats.leastWorn(90),
    ]);
    const next = ov as Partial<StatsOverview>;
    overview.value = {
      totalClothing: next.totalClothing ?? 0,
      totalOutfits: next.totalOutfits ?? 0,
      monthWears: next.monthWears ?? 0,
    };
    mostWorn.value = Array.isArray(mw) ? (mw as MostWornItem[]) : [];
    leastWorn.value = Array.isArray(lw) ? (lw as LeastWornItem[]) : [];
  } catch {
    // 网络 / 后端错误时保留默认占位
  }
});

function goDetail(id: number) {
  uni.navigateTo({ url: `/pages/clothing-detail/index?id=${id}` });
}
</script>

<style scoped>
.stats-page {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  padding: 24rpx;
}
.overview-card {
  display: flex;
  justify-content: space-around;
  padding: 16rpx;
}
.overview-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}
.overview-num {
  font-size: 48rpx;
  font-weight: bold;
  color: #333;
}
.overview-label {
  font-size: 24rpx;
  color: #666;
}
.section {
  display: flex;
  flex-direction: column;
  padding: 8rpx 4rpx;
}
.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 16rpx;
}
.section-tip {
  font-size: 26rpx;
  color: #888;
  line-height: 40rpx;
  padding: 8rpx 0;
}
.list-item {
  display: flex;
  justify-content: space-between;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #eee;
}
.list-item:last-child {
  border-bottom: none;
}
.list-name {
  font-size: 28rpx;
  color: #333;
}
.list-meta {
  font-size: 26rpx;
  color: #888;
}
.empty {
  font-size: 26rpx;
  color: #aaa;
  text-align: center;
  padding: 24rpx 0;
}
</style>

