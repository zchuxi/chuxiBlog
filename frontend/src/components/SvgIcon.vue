<template>
  <svg class="th-tr svg-icon" :style="{ fontSize: size }" aria-hidden="true">
    <use :href="`#icon-${name}`" />
  </svg>
</template>

<script setup>
defineProps({
  name: { type: String, required: true },
  size: { type: String, default: '18px' }
})
</script>
