<script setup lang="ts">
import StatusBadge from '@/components/StatusBadge.vue'
import type { PaymentOrder } from '@/types'

defineProps<{
  canAttempt: boolean
  buttonLabel: string
  attempting: boolean
  showPayment: boolean
  paymentOrder: PaymentOrder | null
  paymentCreating: boolean
  paymentCallbackSubmitting: boolean
}>()

const emit = defineEmits<{
  attempt: []
  'create-payment': []
  'submit-callback': []
}>()
</script>

<template>
  <div class="seckill-section">
    <button class="flat-button seckill-button" type="button" :disabled="!canAttempt" @click="emit('attempt')">
      {{ buttonLabel }}
    </button>

    <div class="payment-panel" v-if="showPayment">
      <div class="payment-panel__header">
        <div>
          <div class="eyebrow">Payment</div>
          <h3 class="payment-panel__title">待支付订单处理</h3>
        </div>
        <StatusBadge label="待支付" tone="amber" />
      </div>
      <div class="payment-panel__actions">
        <button class="flat-button flat-button--secondary" type="button" :disabled="paymentCreating" @click="emit('create-payment')">
          {{ paymentCreating ? '创建中...' : '创建模拟支付单' }}
        </button>
        <button class="flat-button" type="button" :disabled="paymentCallbackSubmitting || !paymentOrder?.transactionNo" @click="emit('submit-callback')">
          {{ paymentCallbackSubmitting ? '回调中...' : '提交模拟支付回调' }}
        </button>
      </div>
      <div class="meta-list" v-if="paymentOrder">
        <div class="meta-row"><span>支付单订单号</span><strong>{{ paymentOrder.orderNo }}</strong></div>
        <div class="meta-row"><span>交易流水号</span><strong>{{ paymentOrder.transactionNo }}</strong></div>
        <div class="meta-row"><span>支付金额</span><strong>{{ paymentOrder.payAmount }}</strong></div>
        <div class="meta-row"><span>支付状态</span><strong>{{ paymentOrder.payStatus }}</strong></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.seckill-section {
  display: grid;
  gap: 0.75rem;
  padding-top: 1rem;
}

.seckill-button {
  justify-self: stretch;
  min-height: 3.5rem;
  font-size: 1.15rem;
  font-weight: 600;
  letter-spacing: 1px;
}

.payment-panel {
  display: grid;
  gap: 0.75rem;
  margin-top: 0.25rem;
  padding: 1rem;
  border: 2px solid rgba(17, 24, 39, 0.18);
  background: rgba(255, 255, 255, 0.5);
}

.payment-panel__header,
.payment-panel__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.payment-panel__title {
  margin: 0.25rem 0 0;
  font-size: 1.35rem;
}

@media (max-width: 960px) {
  .seckill-button {
    justify-self: stretch;
  }

  .payment-panel__header,
  .payment-panel__actions {
    grid-template-columns: 1fr;
    display: grid;
  }
}
</style>
