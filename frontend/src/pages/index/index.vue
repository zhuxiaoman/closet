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

    <!-- AI 搭配入口：樱粉渐变卡片，点击进入 ai-generator 页面 -->
    <view class="ai-cta" data-test="ai-cta" @click="goAi">
      <Icon name="sparkles" :size="36" color="#ffffff" />
      <view class="ai-cta-text">
        <text class="ai-title">AI 搭配助手</text>
        <text class="ai-sub">挑件单品，生成 5 套方案</text>
      </view>
      <Icon name="chevron-right" :size="28" color="rgba(255,255,255,0.85)" />
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { api } from "../../api";
import Icon from "@/components/Icon.vue";

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

function goAi() {
  // 跳到 AI 搭配生成器页（tabBar tab 入口，独立路径）
  uni.navigateTo({ url: "/pages/ai-generator/ai-generator" });
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

/* AI 搭配入口：樱粉渐变卡片 */
.ai-cta {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 24rpx;
  border-radius: 24rpx;
  margin-top: 8rpx;
  background: linear-gradient(
    135deg,
    var(--accent-soft, #fdeef0),
    var(--accent, #d49aa5)
  );
  color: #ffffff;
  box-shadow: var(--shadow-card-soft);
}

.ai-cta-text {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.ai-title {
  font-size: 30rpx;
  font-weight: 600;
  color: var(--accent-strong, #b8808a);
}

.ai-sub {
  font-size: 24rpx;
  color: var(--text-secondary, #8a6d70);
  margin-top: 4rpx;
}
</style>