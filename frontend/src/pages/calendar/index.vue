<template>
  <view class="calendar-page">
    <up-calendar class="calendar" @change="onDateChange" />

    <view v-if="entries.length === 0" class="empty">
      <text>近一个月还没有安排</text>
    </view>

    <view v-for="entry in entries" :key="entry.id" class="entry-row">
      <text>{{ entry.entryDate }}</text>
      <text class="slot">{{ slotLabel(entry.slot) }}</text>
      <text class="arrow">→</text>
      <text>{{ outfitName(entry.outfitId) }}</text>
    </view>

    <view v-if="showCreate" class="modal">
      <view class="modal-card">
        <text class="modal-title">新建日程</text>
        <text class="modal-sub">日期：{{ newDate }}</text>

        <text class="field-label">时段</text>
        <view class="slot-row">
          <button
            v-for="key in SLOT_KEYS"
            :key="key"
            :class="['slot-btn', { active: newSlot === key }]"
            type="button"
            @click="newSlot = key"
          >
            {{ SLOTS[key] }}
          </button>
        </view>

        <text class="field-label">搭配</text>
        <view v-if="outfits.length === 0" class="empty">
          <text>还没有搭配，请先创建</text>
        </view>
        <view
          v-for="o in outfits"
          :key="o.id"
          :class="['outfit-item', { active: newOutfitId === o.id }]"
          @click="newOutfitId = o.id"
        >
          <text>{{ o.name }}</text>
        </view>

        <view class="modal-actions">
          <button type="button" class="btn-secondary" @click="cancelCreate">
            取消
          </button>
          <button
            type="button"
            class="btn-primary"
            :disabled="newOutfitId == null"
            @click="confirmCreate"
          >
            保存
          </button>
        </view>
      </view>
    </view>

    <up-button class="fab" @click="newEntry">+ 新建</up-button>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { api } from '../../api';

interface CalendarEntry {
  id: number;
  entryDate: string;
  slot: string;
  outfitId: number;
}

interface Outfit {
  id: number;
  name: string;
}

type SlotKey = 'morning' | 'afternoon' | 'evening' | 'all_day';

const SLOTS: Record<SlotKey, string> = {
  morning: '上午',
  afternoon: '下午',
  evening: '晚上',
  all_day: '全天',
};

const SLOT_KEYS = Object.keys(SLOTS) as SlotKey[];

const entries = ref<CalendarEntry[]>([]);
const outfits = ref<Outfit[]>([]);

// 创建弹层状态
const showCreate = ref(false);
const newDate = ref('');
const newSlot = ref<SlotKey>('morning');
const newOutfitId = ref<number | null>(null);

function todayISO(offsetDays = 0): string {
  const d = new Date(Date.now() + offsetDays * 86400000);
  return d.toISOString().slice(0, 10);
}

function slotLabel(slot: string): string {
  return (SLOTS as Record<string, string>)[slot] ?? slot;
}

function outfitName(id: number): string {
  return outfits.value.find((o) => o.id === id)?.name ?? '?';
}

async function loadAll() {
  try {
    const data = await api.calendar.range(todayISO(0), todayISO(30));
    const records = (data as { records?: CalendarEntry[] }).records;
    entries.value = records ?? (data as CalendarEntry[]) ?? [];
  } catch {
    entries.value = [];
  }
  try {
    const list = (await api.outfits.list()) as Outfit[];
    outfits.value = list ?? [];
  } catch {
    outfits.value = [];
  }
}

onMounted(loadAll);

function onDateChange(e: { detail?: { year?: number; month?: number; day?: number } }) {
  const detail = e?.detail ?? {};
  if (detail.year && detail.month && detail.day) {
    const m = String(detail.month).padStart(2, '0');
    const d = String(detail.day).padStart(2, '0');
    openCreate(`${detail.year}-${m}-${d}`);
  }
}

function newEntry() {
  openCreate(todayISO(0));
}

function openCreate(date: string) {
  newDate.value = date;
  newSlot.value = 'morning';
  newOutfitId.value = outfits.value[0]?.id ?? null;
  showCreate.value = true;
}

function cancelCreate() {
  showCreate.value = false;
  newOutfitId.value = null;
}

async function confirmCreate() {
  if (newOutfitId.value == null) return;
  try {
    await api.calendar.create({
      entryDate: newDate.value,
      slot: newSlot.value,
      outfitId: newOutfitId.value,
    });
    uni.showToast?.({ title: '已保存', icon: 'success' });
    await loadAll();
  } catch (err) {
    uni.showToast?.({ title: '保存失败', icon: 'none' });
    // eslint-disable-next-line no-console
    console.error('calendar create failed', err);
  } finally {
    showCreate.value = false;
    newOutfitId.value = null;
  }
}
</script>

<style scoped>
.calendar-page {
  display: flex;
  flex-direction: column;
  padding: 24rpx;
  gap: 16rpx;
}
.calendar {
  min-height: 200px;
}
.empty {
  padding: 24rpx;
  color: #999;
  text-align: center;
}
.entry-row {
  display: flex;
  gap: 8rpx;
  align-items: center;
  padding: 16rpx;
  background: #fff;
  border-radius: 8rpx;
  border: 1px solid #eee;
}
.slot {
  color: #555;
}
.arrow {
  color: #999;
}
.modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}
.modal-card {
  background: #fff;
  border-radius: 8rpx;
  padding: 24rpx;
  width: 80%;
  max-width: 480rpx;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}
.modal-title {
  font-size: 32rpx;
  font-weight: bold;
}
.modal-sub {
  color: #666;
  font-size: 24rpx;
}
.field-label {
  font-size: 24rpx;
  color: #333;
  margin-top: 8rpx;
}
.slot-row {
  display: flex;
  gap: 8rpx;
}
.slot-btn {
  flex: 1;
  padding: 8rpx 0;
  border: 1px solid #ddd;
  background: #f8f8f8;
  border-radius: 4rpx;
}
.slot-btn.active {
  background: #2c7be5;
  color: #fff;
  border-color: #2c7be5;
}
.outfit-item {
  padding: 12rpx;
  border: 1px solid #eee;
  border-radius: 4rpx;
}
.outfit-item.active {
  border-color: #2c7be5;
  background: #eaf2fe;
}
.modal-actions {
  display: flex;
  gap: 12rpx;
  margin-top: 16rpx;
}
.btn-primary,
.btn-secondary {
  flex: 1;
  padding: 12rpx 0;
  border-radius: 4rpx;
  border: 1px solid #ddd;
  background: #f8f8f8;
}
.btn-primary {
  background: #2c7be5;
  color: #fff;
  border-color: #2c7be5;
}
.btn-primary[disabled] {
  background: #ccc;
  border-color: #ccc;
}
.fab {
  position: fixed;
  right: 32rpx;
  bottom: 48rpx;
}
</style>
