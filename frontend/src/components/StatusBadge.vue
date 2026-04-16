<script setup lang="ts">
const props = defineProps<{
  label: string
  tone?: 'blue' | 'green' | 'amber' | 'slate'
}>()
</script>

<template>
  <span class="status-badge" :class="`status-badge--${props.tone ?? 'slate'}`">
    {{ props.label }}
  </span>
</template>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 5rem;
  padding: 0.45rem 0.8rem;
  border: 2px solid var(--fg);
  font-size: 0.74rem;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.status-badge--blue {
  background: #dbeafe;
}

.status-badge--green {
  background: #d1fae5;
}

.status-badge--amber {
  background: #fef3c7;
}

.status-badge--slate {
  background: #e5e7eb;
}
</style>
