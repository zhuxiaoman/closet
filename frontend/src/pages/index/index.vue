<template>
  <view class="home">
    <view class="greet">早上好 ☀️</view>

    <up-card>
      <view class="stat-grid">
        <view class="stat">
          <text class="stat-num">{{ stats.totalClothing }}</text>
          <text class="stat-label">衣物</text>
        </view>
        <view class="stat">
          <text class="stat-num">{{ stats.totalOutfits }}</text>
          <text class="stat-label">搭配</text>
        </view>
        <view class="stat">
          <text class="stat-num">{{ stats.monthWears }}</text>
          <text class="stat-label">本月穿</text>
        </view>
      </view>
    </up-card>

    <view class="actions">
      <up-button class="btn-primary" @click="goCloset">打开衣橱</up-button>
      <up-button class="btn-secondary" @click="goCalendar">打开日历</up-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { api } from "../../api";

interface StatsOverview {
  totalClothing: number;
  totalOutfits: number;
  monthWears: number;
}

const stats = ref<StatsOverview>({
  totalClothing: 0,
  totalOutfits: 0,
  monthWears: 0,
});

onMounted(async () => {
  try {
    const data = await api.stats.overview();
    // api/index.ts 的 request<T> 已经解包 envelope，data 即 StatsOverview
    const next = data as Partial<StatsOverview>;
    stats.value = {
      totalClothing: next.totalClothing ?? 0,
      totalOutfits: next.totalOutfits ?? 0,
      monthWears: next.monthWears ?? 0,
    };
  } catch {
    // 网络 / 后端错误时保持默认 0，不阻塞渲染
  }
});

function goCloset() {
  uni.navigateTo({ url: "/pages/closet/index" });
}

function goCalendar() {
  uni.navigateTo({ url: "/pages/calendar/index" });
}
</script>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  gap: 32rpx;
  padding: 24rpx;
  background: var(--bg-secondary);
  min-height: 100vh;
}

.greet {
  font-size: var(--font-h3);
  color: var(--text-secondary);
  padding: 0 8rpx;
}

.stat-grid {
  display: flex;
  justify-content: space-around;
  padding: 24rpx 16rpx;
}

.stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}

.stat-num {
  font-size: 48rpx;
  font-weight: 600;
  color: var(--text-primary);
}

.stat-label {
  font-size: 24rpx;
  color: var(--text-secondary);
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.btn-primary {
  background: var(--color-primary) !important;
  border-color: var(--color-primary) !important;
  color: #ffffff !important;
  border-radius: var(--radius-button) !important;
}

.btn-secondary {
  background: var(--bg-card) !important;
  border-color: var(--border-color) !important;
  color: var(--color-primary) !important;
  border-radius: var(--radius-button) !important;
}
</style>