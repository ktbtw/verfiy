<script setup lang="ts">
defineOptions({ name: 'UiCard' })
defineProps<{ padded?: boolean }>()
</script>

<template>
  <div class="ui-card elev-hover" :class="{ 'ui-card--padded': padded !== false }">
    <slot />
  </div>
  
</template>

<style scoped>
.ui-card { background: var(--surface); box-shadow: var(--elev-2); border-radius: var(--radius-m); -webkit-backdrop-filter: blur(8px); backdrop-filter: blur(8px); }
.ui-card--padded { padding: 16px; }
</style>


