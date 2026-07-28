<template>
  <section class="timeline-section-container">
    <div class="timeline-section-container-header">
      <div class="timeline-section-container-header-main">
        <div class="timeline-section-container-header-title-row">
          <p class="timeline-section-container-header-eyebrow">{{ eyebrow }}</p>
          <h2 v-if="title" class="timeline-section-container-header-title">{{ title }}</h2>
          <slot name="tags" />
        </div>
        <div v-if="$slots.pill" class="timeline-section-container-header-description">
          <div class="timeline-section-container-header-slot-pill"><slot name="pill" /></div>
        </div>
      </div>
      <div v-if="$slots.action" class="timeline-section-container-header-right-action">
        <slot name="action" />
      </div>
    </div>
    <div class="timeline-section-container-content">
      <slot />
    </div>
  </section>
</template>

<script setup>
defineProps({
  eyebrow: { type: String, default: '' },
  title: { type: String, default: '' }
})
</script>
