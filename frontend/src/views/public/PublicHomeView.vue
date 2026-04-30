<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { ArrowRight, BadgeCheck, LayoutPanelTop, Sparkles } from 'lucide-vue-next'
</script>

<template>
  <div class="page-shell">
    <section class="public-hero">
      <div class="public-hero__copy">
        <div class="eyebrow">Flash Sale Platform</div>
        <h1 class="poster-title">高并发秒杀平台</h1>
        <p class="poster-copy">体验极速秒杀，抢购热门商品。查看最新秒杀活动，或者进入管理后台进行配置配置与监控。</p>
        <div class="public-hero__actions">
          <RouterLink class="flat-button" to="/public/activities">
            探索秒杀活动
            <ArrowRight :size="18" />
          </RouterLink>
          <RouterLink class="flat-button flat-button--ghost" to="/login">进入管理后台</RouterLink>
        </div>
      </div>
      <div class="public-hero__grid">
        <article class="flat-panel flat-panel--blue">
          <LayoutPanelTop :size="28" />
          <strong>活动管理</strong>
          <span>创建、发布、下架秒杀活动。</span>
        </article>
        <article class="flat-panel flat-panel--green">
          <BadgeCheck :size="28" />
          <strong>公平抢购</strong>
          <span>高性能架构保障抢购公平。</span>
        </article>
        <article class="flat-panel flat-panel--amber">
          <Sparkles :size="28" />
          <strong>极速体验</strong>
          <span>轻松应对高并发流量。</span>
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
    radial-gradient(circle at 100% 0%, #ffedd5 0%, transparent 50%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.72) 0 28%, transparent 28% 100%),
    #f3f4f6;
}

.public-hero__grid article {
  display: grid;
  gap: 0.8rem;
}

@media (max-width: 960px) {
  .public-hero {
    grid-template-columns: 1fr;
  }
}
</style>
