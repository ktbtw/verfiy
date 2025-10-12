<script setup lang="ts">
defineOptions({ name: 'UiButton' })
import { computed } from 'vue'

const props = defineProps<{
  variant?: 'primary' | 'ghost' | 'danger' | 'soft'
  size?: 'sm' | 'md' | 'lg'
  block?: boolean
  loading?: boolean
  disabled?: boolean
}>()

const cls = computed(() => {
  const v = props.variant || 'primary'
  const s = props.size || 'md'
  return [
    'ui-btn',
    `ui-btn--${v}`,
    `ui-btn--${s}`,
    props.block ? 'ui-btn--block' : '',
  ].join(' ')
})
</script>

<template>
  <button :class="cls" :disabled="disabled || loading">
    <span v-if="loading" class="spinner" style="margin-right:6px"></span>
    <slot />
  </button>
  
</template>

<style scoped>
.ui-btn { display:inline-flex; align-items:center; justify-content:center; gap:6px; border-radius: var(--radius-s); border: 1px solid transparent; cursor: pointer; box-shadow: var(--elev-1); transition: transform .05s var(--ease-smooth), box-shadow var(--dur-normal) var(--ease-smooth), background var(--dur-normal) var(--ease-smooth), color var(--dur-normal) var(--ease-smooth); }
.ui-btn:active { transform: translateY(1px); }
.ui-btn:disabled { opacity: .6; cursor: not-allowed; }

.ui-btn--sm { padding: 6px 10px; font-size: 12px; font-weight:600; }
.ui-btn--md { padding: 10px 14px; font-size: 14px; font-weight:600; }
.ui-btn--lg { padding: 12px 16px; font-size: 16px; font-weight:700; }
.ui-btn--block { width: 100%; }

.ui-btn--primary { background:#111827; color:#fff; }
.ui-btn--primary:hover { background:#0b1220; }
.ui-btn--ghost { background: transparent; color: var(--text-1); border-color: rgba(0,0,0,.08); }
.ui-btn--ghost:hover { background: rgba(0,0,0,.04); }
.ui-btn--danger { background: var(--danger); color:#fff; }
.ui-btn--danger:hover { filter: brightness(.95); }
.ui-btn--soft { background: rgba(79,70,229,.08); color: var(--brand); }
.ui-btn--soft:hover { background: rgba(79,70,229,.15); }

@media (prefers-color-scheme: dark){
  .ui-btn--ghost { color: var(--text-1-dark); border-color: rgba(255,255,255,.12); }
  .ui-btn--ghost:hover { background: rgba(255,255,255,.06); }
}
</style>


