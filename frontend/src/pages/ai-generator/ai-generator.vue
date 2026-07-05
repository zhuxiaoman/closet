<template>
  <view class="page">
    <view class="header">
      <text class="title">AI 搭配</text>
      <text class="sub">从你的衣橱中挑选起点单品，生成 5 套穿搭方案</text>
    </view>

    <view v-if="seedIds.length === 0" class="empty">
      <Icon name="sparkles" :size="48" color="var(--accent, #d49aa5)" />
      <text class="empty-text">请先选择起点单品</text>
      <button class="link" @click="goCloset">去衣橱选择</button>
    </view>

    <AiGenerator
      v-else
      :seed-ids="seedIds"
      :generating="ai.generating"
      @generate="onGenerate"
    />

    <view v-if="ai.lastResult" class="results">
      <text class="section-title">推荐搭配</text>
      <OutfitResultCard
        v-for="(ids, i) in ai.lastResult.outfits"
        :key="i"
        :index="i + 1"
        :generation-id="ai.lastResult.generationId"
        :items="idToNameList(ids)"
        @like="onLike"
        @dislike="onDislike"
        @save="onSave"
      />
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { api } from '@/api';
import { useAiStore } from '@/store/ai';
import type { AiGenerateRequest } from '@/api/ai';
import AiGenerator from '@/components/AiGenerator.vue';
import OutfitResultCard from '@/components/OutfitResultCard.vue';
import Icon from '@/components/Icon.vue';

interface ClothingLite {
  id: number;
  name: string;
}

const ai = useAiStore();
const seedIds = ref<number[]>([]);
const idToName = ref<Map<number, string>>(new Map());

onMounted(async () => {
  try {
    const data = (await api.clothing.list()) as
      | { records?: ClothingLite[] }
      | ClothingLite[];
    const list: ClothingLite[] = Array.isArray(data)
      ? data
      : data.records ?? [];
    idToName.value = new Map(list.map((c) => [c.id, c.name]));
  } catch {
    idToName.value = new Map();
  }
});

function nameOf(id: number): string {
  return idToName.value.get(id) ?? '#' + id;
}

function idToNameList(ids: number[]): string[] {
  return ids.map((id) => nameOf(id));
}

async function onGenerate(req: AiGenerateRequest) {
  await ai.generate(req);
}

async function onLike() {
  if (!ai.lastResult) return;
  await ai.feedback(ai.lastResult.generationId, 'like');
  toast('已喜欢');
}

async function onDislike() {
  if (!ai.lastResult) return;
  await ai.feedback(ai.lastResult.generationId, 'dislike');
  toast('已忽略');
}

function onSave() {
  toast('已收藏');
}

function toast(title: string) {
  if (typeof uni !== 'undefined' && uni.showToast) {
    uni.showToast({ title, icon: 'none' });
  }
}

function goCloset() {
  if (typeof uni !== 'undefined' && uni.switchTab) {
    uni.switchTab({ url: '/pages/closet/index' });
  }
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: var(--bg, #fff7f2);
  padding: 32rpx 24rpx;
}
.header {
  margin-bottom: 32rpx;
}
.title {
  font-size: 48rpx;
  font-weight: 700;
  color: var(--text-primary, #4a3a3a);
  display: block;
}
.sub {
  font-size: 26rpx;
  color: var(--text-secondary, #8a6d70);
  margin-top: 8rpx;
  display: block;
}
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80rpx 0;
  gap: 16rpx;
  background: var(--surface, var(--bg-card, #f9f0ee));
  border-radius: 16rpx;
}
.empty-text {
  color: var(--text-secondary, #8a6d70);
  font-size: 28rpx;
}
.link {
  color: var(--accent, #d49aa5);
  font-size: 28rpx;
  background: transparent;
  border: 0;
  padding: 8rpx 24rpx;
}
.results {
  margin-top: 32rpx;
}
.section-title {
  font-size: 32rpx;
  font-weight: 600;
  color: var(--text-primary, #4a3a3a);
  margin-bottom: 16rpx;
  display: block;
}
</style>
