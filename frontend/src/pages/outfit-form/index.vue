<template>
  <view class="outfit-form">
    <view class="form">
      <view class="field">
        <text class="label">名称</text>
        <input v-model="form.name" class="input" placeholder="给搭配起个名字" />
      </view>
      <view class="field">
        <text class="label">描述</text>
        <textarea
          v-model="form.description"
          class="textarea"
          placeholder="描述一下这套搭配的感受"
        />
      </view>
      <view class="field">
        <text class="label">场合</text>
        <input v-model="form.occasion" class="input" placeholder="例如：通勤 / 正式 / 旅行" />
      </view>
      <view class="field">
        <text class="label">季节</text>
        <input v-model="form.season" class="input" placeholder="例如：夏 / 春 / 秋 / 冬" />
      </view>
      <view class="field row">
        <text class="label">收藏</text>
        <up-checkbox-group>
          <up-checkbox
            :checked="form.isFavorite"
            label="加入收藏"
            :value="true"
            @change="onFavoriteChange"
          />
        </up-checkbox-group>
      </view>
    </view>

    <view class="section">
      <view class="section-header">
        <text class="section-title">搭配衣物</text>
        <up-button @click="openPicker">+ 添加衣物</up-button>
      </view>
      <OutfitCanvas :items="items" />
      <view v-if="items.length > 0" class="items-actions">
        <up-button
          v-for="(it, idx) in items"
          :key="it.id"
          @click="removeAt(idx)"
        >
          移除 {{ it.name ?? ('衣物 #' + it.id) }}
        </up-button>
      </view>
    </view>

    <view v-if="pickerOpen" class="picker-mask" @click="closePicker">
      <view class="picker" @click.stop>
        <view class="picker-header">
          <text class="picker-title">选择衣物</text>
          <up-button @click="closePicker">关闭</up-button>
        </view>
        <view v-if="clothingList.length === 0" class="picker-empty">
          <text>衣物库为空，请先到衣橱添加衣物</text>
        </view>
        <view v-else class="picker-grid">
          <view
            v-for="c in clothingList"
            :key="c.id"
            class="picker-card"
            @click="pickClothing(c.id)"
          >
            <text class="picker-card-name">{{ c.name }}</text>
          </view>
        </view>
      </view>
    </view>

    <view class="footer">
      <up-button @click="submit">{{ isEdit ? '保存修改' : '保存搭配' }}</up-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { api } from '../../api';
import OutfitCanvas, { type OutfitItem } from '../../components/OutfitCanvas.vue';

interface ClothingRecord {
  id: number;
  name: string;
  mainImageKey?: string;
}

interface OutfitDetail {
  id: number;
  name?: string;
  description?: string;
  occasion?: string;
  season?: string;
  isFavorite?: boolean;
  items?: OutfitItem[];
}

const outfitId = ref<number | null>(null);
const isEdit = computed(() => outfitId.value !== null);

const form = reactive({
  name: '',
  description: '',
  occasion: '',
  season: '',
  isFavorite: false,
});

const items = ref<OutfitItem[]>([]);
const clothingList = ref<ClothingRecord[]>([]);

const pickerOpen = ref(false);

function readQueryId(): number | null {
  const g = globalThis as unknown as { __closetOutfitFormEditId?: number | string | null };
  const fallback = g.__closetOutfitFormEditId;
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
  // Load clothing list for picker
  try {
    const data = (await api.clothing.list()) as { records?: ClothingRecord[] };
    clothingList.value = data.records ?? [];
  } catch {
    clothingList.value = [];
  }

  // Load existing outfit if editing
  const id = readQueryId();
  if (id === null) return;
  outfitId.value = id;
  try {
    const data = (await api.outfits.get(id)) as OutfitDetail;
    form.name = data.name ?? '';
    form.description = data.description ?? '';
    form.occasion = data.occasion ?? '';
    form.season = data.season ?? '';
    form.isFavorite = Boolean(data.isFavorite);
    items.value = (data.items ?? []).map((it, idx) => ({
      ...it,
      sortOrder: it.sortOrder ?? idx,
    }));
  } catch {
    // edit load failed, keep blank
  }
});

function onFavoriteChange(value: string | number | boolean) {
  form.isFavorite = Boolean(value);
}

function openPicker() {
  pickerOpen.value = true;
}

function closePicker() {
  pickerOpen.value = false;
}

function pickClothing(id: number) {
  const target = clothingList.value.find((c) => c.id === id);
  if (!target) return;
  if (items.value.some((i) => i.id === id)) {
    uni.showToast({ title: '已添加过该衣物', icon: 'none' });
    return;
  }
  const next: OutfitItem = {
    id: target.id,
    name: target.name,
    mainImageKey: target.mainImageKey,
    sortOrder: items.value.length,
  };
  items.value = [...items.value, next];
  pickerOpen.value = false;
}

function removeAt(idx: number) {
  const next = items.value.filter((_, i) => i !== idx);
  items.value = next.map((it, i) => ({ ...it, sortOrder: i }));
}

async function submit() {
  if (!form.name.trim()) {
    uni.showToast({ title: '请填写搭配名称', icon: 'none' });
    return;
  }
  const payload = {
    name: form.name.trim(),
    description: form.description,
    occasion: form.occasion,
    season: form.season,
    isFavorite: form.isFavorite,
    clothingIds: items.value.map((i) => i.id),
  };
  try {
    if (isEdit.value && outfitId.value !== null) {
      await api.outfits.update(outfitId.value, payload);
    } else {
      await api.outfits.create(payload);
    }
    uni.showToast({ title: '保存成功' });
    uni.navigateBack();
  } catch {
    uni.showToast({ title: '保存失败', icon: 'none' });
  }
}
</script>

<style scoped>
.outfit-form {
  display: flex;
  flex-direction: column;
  padding: 16rpx;
  background: #fafafa;
  min-height: 100vh;
}
.form {
  background: #fff;
  border-radius: 8rpx;
  padding: 16rpx;
}
.field {
  display: flex;
  flex-direction: column;
  margin-bottom: 16rpx;
}
.field.row {
  flex-direction: row;
  align-items: center;
  gap: 16rpx;
}
.label {
  font-size: 26rpx;
  color: #666;
  margin-bottom: 8rpx;
}
.input,
.textarea {
  width: 100%;
  padding: 12rpx 16rpx;
  border: 1rpx solid #e5e5e5;
  border-radius: 6rpx;
  font-size: 28rpx;
  background: #fff;
  box-sizing: border-box;
}
.textarea {
  min-height: 120rpx;
}
.section {
  margin-top: 16rpx;
  background: #fff;
  border-radius: 8rpx;
  padding: 16rpx;
}
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8rpx;
}
.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
}
.items-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
  margin-top: 8rpx;
}
.picker-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: flex-end;
  z-index: 999;
}
.picker {
  width: 100%;
  max-height: 70vh;
  background: #fff;
  border-top-left-radius: 16rpx;
  border-top-right-radius: 16rpx;
  padding: 16rpx;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}
.picker-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
}
.picker-title {
  font-size: 30rpx;
  font-weight: 600;
}
.picker-empty {
  padding: 48rpx 0;
  text-align: center;
  color: #999;
}
.picker-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}
.picker-card {
  width: calc(50% - 6rpx);
  padding: 16rpx;
  border: 1rpx solid #eee;
  border-radius: 6rpx;
  box-sizing: border-box;
}
.picker-card-name {
  font-size: 26rpx;
  color: #333;
}
.footer {
  padding: 24rpx 0 32rpx;
}
</style>
