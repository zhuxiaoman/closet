<template>
  <view class="clothing-form">
    <view class="field">
      <text class="label">名称 *</text>
      <input
        v-model="form.name"
        class="input"
        placeholder="给这件衣物起个名字"
        data-testid="name-input"
      />
    </view>
    <view class="field">
      <text class="label">品牌</text>
      <input v-model="form.brand" class="input" placeholder="例如 Uniqlo" />
    </view>
    <view class="field-row">
      <view class="field">
        <text class="label">主色</text>
        <input v-model="form.colorPrimary" class="input" placeholder="例如 白色" />
      </view>
      <view class="field">
        <text class="label">配色</text>
        <input v-model="form.colorSecondary" class="input" placeholder="例如 蓝色" />
      </view>
    </view>
    <view class="field-row">
      <view class="field">
        <text class="label">尺码</text>
        <input v-model="form.size" class="input" placeholder="例如 M" />
      </view>
      <view class="field">
        <text class="label">入手价</text>
        <input
          v-model.number="form.purchasePrice"
          class="input"
          type="number"
          placeholder="例如 199"
        />
      </view>
    </view>
    <view class="field-row">
      <view class="field">
        <text class="label">入手日期</text>
        <input
          v-model="form.purchaseDate"
          class="input"
          placeholder="2026-07-01"
        />
      </view>
      <view class="field">
        <text class="label">季节</text>
        <picker :range="seasonOptions" @change="onSeasonChange">
          <view class="picker-display">
            <text>{{ form.season || '全部' }}</text>
          </view>
        </picker>
      </view>
    </view>
    <view class="field">
      <text class="label">备注</text>
      <textarea
        v-model="form.notes"
        class="textarea"
        placeholder="随手记点什么"
      />
    </view>
    <view class="field">
      <text class="label">分类</text>
      <CategoryPicker
        v-model="form.categoryIds"
        :categories="categories"
      />
    </view>
    <view class="field">
      <text class="label">标签</text>
      <TagPicker v-model="form.tagIds" :tags="tags" />
    </view>
    <view class="field">
      <text class="label">图片</text>
      <ImageUploader
        :clothing-id="clothingId"
        :images="form.images"
        @uploaded="onUploaded"
      />
    </view>
    <view class="footer">
      <up-button @click="onSubmit">保存</up-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import CategoryPicker from '../../components/CategoryPicker.vue';
import TagPicker from '../../components/TagPicker.vue';
import ImageUploader from '../../components/ImageUploader.vue';
import { api } from '../../api';

interface Category {
  id: number;
  name: string;
}
interface Tag {
  id: number;
  name: string;
}
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
  purchasePrice?: number | null;
  purchaseDate?: string;
  season?: string | null;
  notes?: string;
  categoryIds?: number[];
  tagIds?: number[];
  images?: ClothingImage[];
}

type GlobalWithClosetForm = {
  __closetFormEditId?: number | string | null;
};
type GlobalWithPages = {
  getCurrentPages?: () => Array<{ options?: Record<string, unknown> }>;
};

const seasonOptions = ['全部', 'spring', 'summer', 'fall', 'winter'];

const clothingId = ref<number>(0);
const isEdit = ref(false);
const categories = ref<Category[]>([]);
const tags = ref<Tag[]>([]);

const form = reactive<{
  name: string;
  brand: string;
  colorPrimary: string;
  colorSecondary: string;
  size: string;
  purchasePrice: number | null;
  purchaseDate: string;
  season: string | null;
  notes: string;
  categoryIds: number[];
  tagIds: number[];
  images: ClothingImage[];
}>({
  name: '',
  brand: '',
  colorPrimary: '',
  colorSecondary: '',
  size: '',
  purchasePrice: null,
  purchaseDate: '',
  season: null,
  notes: '',
  categoryIds: [],
  tagIds: [],
  images: [],
});

function readQueryId(): number | null {
  const g = globalThis as unknown as GlobalWithClosetForm;
  const fallback = g.__closetFormEditId;
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

async function loadCategoriesAndTags() {
  try {
    const [cats, tagList] = await Promise.all([
      api.categories.list(),
      api.tags.list(),
    ]);
    categories.value = (cats as Category[]) ?? [];
    tags.value = (tagList as Tag[]) ?? [];
  } catch {
    categories.value = [];
    tags.value = [];
  }
}

onMounted(async () => {
  await loadCategoriesAndTags();
  const id = readQueryId();
  if (id === null) return;
  isEdit.value = true;
  clothingId.value = id;
  try {
    const detail = (await api.clothing.get(id)) as ClothingDetail;
    form.name = detail.name ?? '';
    form.brand = detail.brand ?? '';
    form.colorPrimary = detail.colorPrimary ?? '';
    form.colorSecondary = detail.colorSecondary ?? '';
    form.size = detail.size ?? '';
    form.purchasePrice = detail.purchasePrice ?? null;
    form.purchaseDate = detail.purchaseDate ?? '';
    form.season = detail.season ?? null;
    form.notes = detail.notes ?? '';
    form.categoryIds = Array.isArray(detail.categoryIds)
      ? [...detail.categoryIds]
      : [];
    form.tagIds = Array.isArray(detail.tagIds) ? [...detail.tagIds] : [];
    form.images = Array.isArray(detail.images) ? [...detail.images] : [];
  } catch {
    // 编辑加载失败时保留空表单,允许用户重新输入
  }
});

function onSeasonChange(e: { detail: { value: number } }) {
  const idx = e.detail.value;
  form.season = idx === 0 ? null : seasonOptions[idx];
}

function onUploaded(image: ClothingImage) {
  form.images = [...form.images, image];
}

function validate(): string | null {
  if (!form.name.trim()) return '请填写名称';
  return null;
}

async function onSubmit() {
  const err = validate();
  if (err) {
    uni.showToast({ title: err, icon: 'none' });
    return;
  }
  const payload = {
    name: form.name.trim(),
    brand: form.brand.trim() || null,
    colorPrimary: form.colorPrimary.trim() || null,
    colorSecondary: form.colorSecondary.trim() || null,
    size: form.size.trim() || null,
    purchasePrice: form.purchasePrice,
    purchaseDate: form.purchaseDate.trim() || null,
    season: form.season,
    notes: form.notes.trim() || null,
    categoryIds: [...form.categoryIds],
    tagIds: [...form.tagIds],
    images: form.images.map((i) => i.storageKey),
  };
  try {
    if (isEdit.value && clothingId.value > 0) {
      await api.clothing.update(clothingId.value, payload);
    } else {
      await api.clothing.create(payload);
    }
    uni.showToast({ title: '保存成功' });
    uni.navigateBack();
  } catch {
    uni.showToast({ title: '保存失败', icon: 'none' });
  }
}
</script>

<style scoped>
.clothing-form {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  padding: 24rpx;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}
.field-row {
  display: flex;
  gap: 16rpx;
}
.field-row .field {
  flex: 1;
}
.label {
  font-size: 26rpx;
  color: #666;
}
.input,
.textarea {
  border: 1rpx solid #ddd;
  border-radius: 8rpx;
  padding: 16rpx;
  font-size: 28rpx;
  background: #fff;
}
.textarea {
  min-height: 120rpx;
}
.picker-display {
  border: 1rpx solid #ddd;
  border-radius: 8rpx;
  padding: 16rpx;
  background: #fff;
  font-size: 28rpx;
}
.footer {
  padding: 24rpx 0;
}
</style>
