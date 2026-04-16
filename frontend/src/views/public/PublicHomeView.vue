<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { ArrowRight, BadgeCheck, LayoutPanelTop, Sparkles } from 'lucide-vue-next'
</script>

<template>
  <div class="page-shell">
    <section class="public-hero">
      <div class="public-hero__copy">
        <div class="eyebrow">Poster Homepage</div>
        <h1 class="poster-title">把后台可配置能力，翻译成用户能一眼看懂的活动入口。</h1>
        <p class="poster-copy">
          当前用户侧接口仍未开放，所以这里用本地 mock 数据搭出完整展示壳子。风格和后台共用同一套 token，但用更大的色块、几何装饰和更强的海报感去建立记忆点。
        </p>
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
          <strong>后台真实联调</strong>
          <span>登录、活动管理、发布与下线全部走现有后端接口。</span>
        </article>
        <article class="flat-panel flat-panel--green">
          <BadgeCheck :size="28" />
          <strong>用户侧完整展示</strong>
          <span>首页、活动列表、详情页均可独立演示流程与样式。</span>
        </article>
        <article class="flat-panel flat-panel--amber public-hero__accent">
          <Sparkles :size="30" />
          <strong>Flat Design / Poster</strong>
          <span>零阴影、零玻璃感，用粗边界和高对比结构建立秩序。</span>
        </article>
      </div>
    </section>

    <section class="flat-grid flat-grid--3">
      <article class="flat-panel">
        <div class="eyebrow">Flow 01</div>
        <h2>配置活动</h2>
        <p>后台编辑页对齐后端字段，统一枚举与时间格式。</p>
      </article>
      <article class="flat-panel flat-panel--blue">
        <div class="eyebrow">Flow 02</div>
        <h2>发布或定时</h2>
        <p>发布动作会直接触发当前已有的发布逻辑和调度器策略。</p>
      </article>
      <article class="flat-panel flat-panel--green">
        <div class="eyebrow">Flow 03</div>
        <h2>公开展示</h2>
        <p>用户侧壳子用 mock 数据先把品牌展示和 CTA 节奏铺平。</p>
      </article>
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
