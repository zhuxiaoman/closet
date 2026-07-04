<template>
  <view class="category-picker">
    <up-checkbox-group v-model="selected" @change="onChange">
      <up-checkbox
        v-for="c in categories"
        :key="c.id"
        :label="c.name"
        :value="c.id"
      />
    </up-checkbox-group>
  </view>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';

interface Category {
  id: number;
  name: string;
}

const props = defineProps<{
  categories: Category[];
  modelValue: number[];
}>();
const emit = defineEmits<{ 'update:modelValue': [v: number[]] }>();

// 内部 selected 与外部 modelValue 双向同步：
//   - selected 变 → emit update:modelValue，父组件 v-model 把 props.modelValue 反过来
//   - props.modelValue 变（例如父组件重置） → selected 跟随
// 内部状态用副本避免直接拿到 props 引用
const selected = ref<number[]>([...(props.modelValue || [])]);
watch(selected, (v) => emit('update:modelValue', v));
watch(
  () => props.modelValue,
  (v) => {
    selected.value = Array.isArray(v) ? [...v] : [];
  },
);

function onChange() {
  // 占位，uView Plus 通过 v-model 自动同步内部状态；此处留给将来扩展
}
</script>

<style scoped>
.category-picker {
  padding: 16rpx 24rpx;
}
</style>
