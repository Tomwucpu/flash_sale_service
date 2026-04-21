<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { ArrowRight, BadgeCheck, LayoutPanelTop, Sparkles } from 'lucide-vue-next'
</script>

<template>
  <div class="page-shell">
    <section class="public-hero">
      <div class="public-hero__copy">
        <div class="eyebrow">Poster Homepage</div>
        <h1 class="poster-title">活动入口</h1>
        <p class="poster-copy">查看公开活动，或进入后台管理。</p>
        <div class="public-hero__actions">
          <RouterLink class="flat-button" to="/public/activities">
            查看活动列表
            <ArrowRight :size="18" />
          </RouterLink>
          <RouterLink class="flat-button flat-button--ghost" to="/login">进入后台联调</RouterLink>
        </div>
      </div>
      <div class="public-hero__grid">
        <article class="flat-panel flat-panel--blue">
          <LayoutPanelTop :size="28" />
          <strong>后台管理</strong>
          <span>创建、发布、下线。</span>
        </article>
        <article class="flat-panel flat-panel--green">
          <BadgeCheck :size="28" />
          <strong>公开活动</strong>
          <span>列表与详情。</span>
        </article>
        <article class="flat-panel flat-panel--amber public-hero__accent">
          <Sparkles :size="30" />
          <strong>Flat Poster</strong>
          <span>简洁直观。</span>
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.public-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.85fr);
  gap: 1rem;
}

.public-hero__copy,
.public-hero__grid {
  padding: 2rem;
  border: 2px solid var(--fg);
}

.public-hero__copy {
  display: grid;
  gap: 1.25rem;
  background: #dbeafe;
}

.public-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
}

.public-hero__grid {
  display: grid;
  gap: 1rem;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.72) 0 28%, transparent 28% 100%),
    #f3f4f6;
}

.public-hero__grid article {
  display: grid;
  gap: 0.8rem;
}

.public-hero__accent {
  min-height: 220px;
}

@media (max-width: 960px) {
  .public-hero {
    grid-template-columns: 1fr;
  }
}
</style>
