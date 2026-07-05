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
import { computed } from 'vue';

interface Category {
  id: number;
  name: string;
}

const props = defineProps<{
  categories: Category[];
  modelValue: number[];
}>();
const emit = defineEmits<{ 'update:modelValue': [v: number[]] }>();

// Unidirectional data flow: `selected` forwards props.modelValue.
// The template's v-model triggers the setter on assignment -> emit ->
// parent responds -> props.modelValue updates -> getter returns the
// new value. The component owns no local state, which removes the
// recursive-update warning that the previous ref + dual-watch could
// emit during test teardown.
const selected = computed<number[]>({
  get: () => (Array.isArray(props.modelValue) ? props.modelValue : []),
  set: (v) => emit('update:modelValue', Array.isArray(v) ? v : []),
});

function onChange() {
  // 占位，uView Plus 通过 v-model 自动同步内部状态；此处留给将来扩展
}
</script>

<style scoped>
.category-picker {
  padding: 16rpx 24rpx;
}
</style>
