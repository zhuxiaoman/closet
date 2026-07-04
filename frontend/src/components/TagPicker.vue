<template>
  <view class="tag-picker">
    <up-checkbox-group v-model="selected" @change="onChange">
      <up-checkbox
        v-for="t in tags"
        :key="t.id"
        :label="t.name"
        :value="t.id"
      />
    </up-checkbox-group>
  </view>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';

interface Tag {
  id: number;
  name: string;
}

const props = defineProps<{
  tags: Tag[];
  modelValue: number[];
}>();
const emit = defineEmits<{ 'update:modelValue': [v: number[]] }>();

const selected = ref<number[]>([...(props.modelValue || [])]);
watch(selected, (v) => emit('update:modelValue', v));
watch(
  () => props.modelValue,
  (v) => {
    selected.value = Array.isArray(v) ? [...v] : [];
  },
);

function onChange() {
  // 占位，uView Plus 通过 v-model 自动同步内部状态
}
</script>

<style scoped>
.tag-picker {
  padding: 16rpx 24rpx;
}
</style>
