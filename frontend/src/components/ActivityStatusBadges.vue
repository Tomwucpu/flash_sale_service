<script setup lang="ts">
import StatusBadge from '@/components/StatusBadge.vue'
import { getPhaseLabel, getPublishStatusLabel } from '@/utils/activity'
import type { ActivityPhase, PublishStatus } from '@/types'

defineProps<{
  publishStatus: PublishStatus
  phase: ActivityPhase
}>()

function publishStatusTone(status: PublishStatus): 'blue' | 'green' | 'amber' | 'slate' {
  if (status === 'PUBLISHED') return 'green'
  if (status === 'UNPUBLISHED') return 'amber'
  return 'slate'
}

function phaseTone(phase: ActivityPhase): 'blue' | 'green' | 'amber' | 'slate' {
  if (phase === 'ONGOING') return 'blue'
  if (phase === 'PREVIEW') return 'amber'
  return 'slate'
}
</script>

<template>
  <div class="badge-stack">
    <StatusBadge :label="getPublishStatusLabel($props.publishStatus)" :tone="publishStatusTone($props.publishStatus)" />
    <StatusBadge :label="getPhaseLabel($props.phase)" :tone="phaseTone($props.phase)" />
  </div>
</template>

<style scoped>
.badge-stack {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}
</style>
