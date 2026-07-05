<template>
  <component :is="icon" :size="size" :stroke-width="strokeWidth" :color="color" />
</template>

<script setup lang="ts">
import { computed } from "vue";
import * as LucideIcons from "lucide-vue-next";

const props = withDefaults(
  defineProps<{
    name: string;
    size?: number;
    strokeWidth?: number;
    color?: string;
  }>(),
  {
    size: 20,
    strokeWidth: 1.5,
    color: "currentColor",
  }
);

const icon = computed(() => {
  const pascal = props.name
    .split("-")
    .map((s) => s.charAt(0).toUpperCase() + s.slice(1))
    .join("");
  return (LucideIcons as any)[pascal] || LucideIcons.HelpCircle;
});
</script>