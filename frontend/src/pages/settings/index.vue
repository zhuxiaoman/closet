<template>
  <view class="settings-page">
    <up-card>
      <view class="section">
        <text class="section-title">分类管理</text>
        <view
          v-for="cat in categories"
          :key="cat.id"
          class="list-row"
        >
          <text class="row-name">{{ cat.name }}</text>
          <up-button class="row-btn" @click="deleteCategory(cat.id)">
            删除
          </up-button>
        </view>
        <text v-if="!categories.length" class="empty">暂无分类</text>
        <up-button class="add-btn" @click="addCategory">
          + 新增分类
        </up-button>
      </view>
    </up-card>

    <up-card>
      <view class="section">
        <text class="section-title">标签管理</text>
        <view
          v-for="tag in tags"
          :key="tag.id"
          class="list-row"
        >
          <text class="row-name">{{ tag.name }}</text>
          <up-button class="row-btn" @click="deleteTag(tag.id)">
            删除
          </up-button>
        </view>
        <text v-if="!tags.length" class="empty">暂无标签</text>
        <up-button class="add-btn" @click="addTag">+ 新增标签</up-button>
      </view>
    </up-card>

    <up-button class="export-btn" @click="exportData">数据导出</up-button>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { api } from '../../api';

interface NamedItem {
  id: number;
  name: string;
}

const categories = ref<NamedItem[]>([]);
const tags = ref<NamedItem[]>([]);

async function loadCategories() {
  try {
    const list = (await api.categories.list()) as NamedItem[];
    categories.value = Array.isArray(list) ? list : [];
  } catch {
    categories.value = [];
  }
}

async function loadTags() {
  try {
    const list = (await api.tags.list()) as NamedItem[];
    tags.value = Array.isArray(list) ? list : [];
  } catch {
    tags.value = [];
  }
}

onMounted(async () => {
  await Promise.all([loadCategories(), loadTags()]);
});

async function deleteCategory(id: number) {
  await api.categories.delete(id);
  await loadCategories();
}

async function deleteTag(id: number) {
  await api.tags.delete(id);
  await loadTags();
}

interface ModalResult {
  confirm: boolean;
  content?: string;
}

async function addCategory() {
  const res = (await uni.showModal({
    title: '新增分类',
    editable: true,
    placeholderText: '请输入分类名',
  })) as ModalResult;
  if (res.confirm && res.content && res.content.trim()) {
    await api.categories.create({ name: res.content.trim() });
    await loadCategories();
  }
}

async function addTag() {
  const res = (await uni.showModal({
    title: '新增标签',
    editable: true,
    placeholderText: '请输入标签名',
  })) as ModalResult;
  if (res.confirm && res.content && res.content.trim()) {
    await api.tags.create({ name: res.content.trim() });
    await loadTags();
  }
}

function exportData() {
  // MVP：暂留空，仅 toast 提示
  uni.showToast({ title: '导出功能开发中', icon: 'none' });
}
</script>

<style scoped>
.settings-page {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  padding: 24rpx;
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
.list-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #eee;
}
.list-row:last-of-type {
  border-bottom: none;
}
.row-name {
  font-size: 28rpx;
  color: #333;
}
.row-btn {
  font-size: 24rpx;
  color: #e64340;
  background: #fff;
  border: 1rpx solid #e64340;
  border-radius: 6rpx;
  padding: 4rpx 16rpx;
}
.add-btn {
  margin-top: 16rpx;
  font-size: 28rpx;
  color: #2979ff;
  background: #fff;
  border: 1rpx dashed #2979ff;
  border-radius: 6rpx;
  padding: 16rpx 0;
}
.empty {
  font-size: 26rpx;
  color: #aaa;
  text-align: center;
  padding: 24rpx 0;
}
.export-btn {
  margin-top: 24rpx;
  font-size: 30rpx;
  color: #fff;
  background: #2979ff;
  border: none;
  border-radius: 8rpx;
  padding: 24rpx 0;
}
</style>

