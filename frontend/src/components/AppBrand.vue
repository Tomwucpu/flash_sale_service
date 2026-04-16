<template>
  <div class="app-brand">
    <span class="app-brand__mark">FS</span>
    <div>
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

.app-brand__title {
  font-size: 0.95rem;
  font-weight: 800;
  line-height: 1.1;
}

.app-brand__subtitle {
  margin-top: 0.15rem;
  color: var(--fg-soft);
  font-size: 0.72rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
</style>
