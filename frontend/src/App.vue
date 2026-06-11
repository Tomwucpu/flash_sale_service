<template>
  <ElConfigProvider :message="elementPlusConfig.message">
    <RouterView />
  </ElConfigProvider>
</template>

<script setup lang="ts">
import { elementPlusConfig } from '@/config/ui'
</script>
