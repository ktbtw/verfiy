<script setup lang="ts">
defineOptions({ name: 'UiInput' })
const props = defineProps<{ modelValue?: any; type?: string; placeholder?: string; modelModifiers?: Record<string, boolean> }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: any): void }>()

function onInput(e: Event) {
  const target = e.target as HTMLInputElement
  const raw = target.value
  const isNumber = props.type === 'number' || (props.modelModifiers && props.modelModifiers.number)
  if (isNumber) {
    emit('update:modelValue', raw === '' ? null : Number(raw))
  } else {
    emit('update:modelValue', raw)
  }
}
</script>

<template>
  <input :type="type || 'text'" :placeholder="placeholder" :value="modelValue ?? ''" @input="onInput" class="ui-input" />
  
</template>

<style scoped>
.ui-input { width: 100%; padding: 10px 12px; border-radius: var(--radius-s); border: 1px solid rgba(0,0,0,.08); background: rgba(255,255,255,.8); color: var(--text-1); transition: box-shadow .2s var(--ease-smooth), border-color .2s var(--ease-smooth); }
.ui-input::placeholder { color: var(--text-2); }
.ui-input:focus { outline: none; border-color: rgba(79,70,229,.35); box-shadow: 0 0 0 4px rgba(79,70,229,.15); }
@media (prefers-color-scheme: dark){ .ui-input { background: rgba(255,255,255,.06); border-color: rgba(255,255,255,.12); color: var(--text-1-dark); } }
</style>


