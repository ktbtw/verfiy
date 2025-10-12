<script setup lang="ts">
defineOptions({ name: 'UiPageHeader' })
defineProps<{ title: string }>()
</script>

<template>
  <div class="page-header">
    <div class="page-title">{{ title }}</div>
    <div class="actions"><slot name="actions" /></div>
  </div>
  
</template>

<style scoped>
.page-header { display:flex; align-items:center; justify-content:space-between; margin-bottom: 16px; }
</style>


