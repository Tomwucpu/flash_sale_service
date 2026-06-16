<script setup lang="ts">
import StatusBadge from '@/components/StatusBadge.vue'
import {
  codeStatusTone,
  getCodeStatusLabel,
  getOrderStatusLabel,
  getPayStatusLabel,
  orderStatusTone,
  payStatusTone,
} from '@/utils/order'

defineProps<{
  orderStatus: string
  payStatus: string
  codeStatus: string
}>()
</script>

<template>
  <div class="badge-stack">
    <StatusBadge :label="getOrderStatusLabel($props.orderStatus)" :tone="orderStatusTone($props.orderStatus)" />
    <StatusBadge :label="getPayStatusLabel($props.payStatus)" :tone="payStatusTone($props.payStatus)" />
    <StatusBadge :label="getCodeStatusLabel($props.codeStatus)" :tone="codeStatusTone($props.codeStatus)" />
  </div>
</template>

<style scoped>
.badge-stack {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}
</style>
