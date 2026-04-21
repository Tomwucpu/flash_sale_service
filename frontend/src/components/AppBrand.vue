<script setup lang="ts">
withDefaults(
  defineProps<{
    compact?: boolean
  }>(),
  {
    compact: false,
  },
)
</script>

<template>
  <div class="app-brand" :class="{ 'app-brand--compact': compact }">
    <span class="app-brand__mark">FS</span>
    <div class="app-brand__text">
      <div class="app-brand__title">Flash Sale Service</div>
      <div class="app-brand__subtitle">Flat Poster Console</div>
    </div>
  </div>
</template>

<style scoped>
.app-brand {
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
  transition: gap 0.24s ease;
}

.app-brand__mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.75rem;
  height: 2.75rem;
  background: var(--primary);
  color: white;
  font-size: 1rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  border: 2px solid var(--fg);
}

.app-brand__text {
  overflow: hidden;
  max-width: 220px;
  opacity: 1;
  transform: translateX(0);
  transition:
    max-width 0.24s ease,
    opacity 0.18s ease,
    transform 0.24s ease;
}

.app-brand__title {
  font-size: 0.95rem;
  font-weight: 800;
  line-height: 1.1;
  white-space: nowrap;
}

.app-brand__subtitle {
  margin-top: 0.15rem;
  color: var(--fg-soft);
  font-size: 0.72rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  white-space: nowrap;
}

.app-brand--compact {
  gap: 0;
}

.app-brand--compact .app-brand__text {
  max-width: 0;
  opacity: 0;
  transform: translateX(-4px);
}
</style>
