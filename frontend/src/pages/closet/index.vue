<template>
  <view class="closet">
    <FilterBar :filter="filter" @change="onFilter" />
    <view v-if="loading" class="loading">
      <text>加载中…</text>
    </view>
    <view v-else-if="list.length === 0" class="empty">
      <text>暂无衣物，点击下方按钮添加第一件吧</text>
    </view>
    <view v-else class="grid">
      <ClothingCard
        v-for="c in list"
        :key="c.id"
        :clothing="c"
        @click="goDetail(c.id)"
      />
    </view>
    <view class="footer">
      <up-button @click="goForm()">+ 添加衣物</up-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue';
import FilterBar, { type ClothingFilter } from '../../components/FilterBar.vue';
import ClothingCard from '../../components/ClothingCard.vue';
import { api } from '../../api';

interface ClothingItem {
  id: number;
  name: string;
  mainImageKey?: string;
}

const filter = reactive<ClothingFilter>({ season: null });
const list = ref<ClothingItem[]>([]);
const loading = ref(false);

async function load() {
  loading.value = true;
  try {
    const data = await api.clothing.list(filter as unknown as Record<string, unknown>);
    const records = (data as { records?: ClothingItem[] }).records ?? [];
    list.value = records;
  } catch {
    list.value = [];
  } finally {
    loading.value = false;
  }
}

onMounted(load);

async function onFilter(next: ClothingFilter) {
  Object.assign(filter, next);
  await load();
}

function goDetail(id: number) {
  uni.navigateTo({ url: '/pages/clothing-detail/index?id=' + id });
}

function goForm() {
  uni.navigateTo({ url: '/pages/clothing-form/index' });
}
</script>

<style scoped>
.closet {
  display: flex;
  flex-direction: column;
  padding: 16rpx;
}
.grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  padding: 16rpx 0;
}
.empty,
.loading {
  padding: 64rpx 0;
  text-align: center;
  color: #999;
}
.footer {
  padding: 16rpx 0 32rpx;
}
</style>
