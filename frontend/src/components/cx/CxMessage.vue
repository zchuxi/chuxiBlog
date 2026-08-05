<template>
  <Teleport to="body">
    <div class="cx-message-container">
      <transition-group name="cx-message-slide">
        <div v-for="m in messages" :key="m.id" class="cx-message-item" :class="`cx-message-${m.type}`">
          <span>{{ m.text }}</span>
        </div>
      </transition-group>
    </div>
  </Teleport>
</template>

<script setup>
import { ref } from 'vue'

const messages = ref([])
let msgSeq = 0

function pushMessage(type, text, duration = 2600) {
  const id = ++msgSeq
  messages.value.push({ id, type, text })
  if (duration > 0) {
    setTimeout(() => {
      messages.value = messages.value.filter(m => m.id !== id)
    }, duration)
  }
}

function clearMessages() {
  messages.value = []
}

defineExpose({ pushMessage, clearMessages })
</script>

<style>
.cx-message-container {
  position: fixed;
  top: 24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 99999;
  display: flex;
  flex-direction: column;
  gap: 10px;
  pointer-events: none;
}
.cx-message-item {
  padding: 10px 20px;
  border-radius: 12px;
  font-size: 14.5px;
  font-weight: 600;
  line-height: 1.5;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
  pointer-events: auto;
  white-space: nowrap;
}
.cx-message-success {
  background: linear-gradient(135deg, #eff7f2, #e0f0e6);
  border: 1px solid #c9e0d2;
  color: #2f654c;
}
.cx-message-info {
  background: linear-gradient(135deg, #eef5fd, #e0ecf9);
  border: 1px solid #cadbee;
  color: #264968;
}
.cx-message-error {
  background: linear-gradient(135deg, #fff2f0, #f6dfdb);
  border: 1px solid #e5c2bc;
  color: #8b4740;
}
html.dark .cx-message-success {
  background: linear-gradient(135deg, rgba(55, 108, 85, .22), rgba(37, 76, 63, .32));
  border-color: rgba(111, 174, 145, .26);
  color: #dff2e9;
}
html.dark .cx-message-info {
  background: linear-gradient(135deg, rgba(73, 104, 140, .22), rgba(52, 76, 104, .32));
  border-color: rgba(126, 160, 198, .28);
  color: #deebf8;
}
html.dark .cx-message-error {
  background: linear-gradient(135deg, rgba(116, 58, 54, .24), rgba(85, 39, 39, .34));
  border-color: rgba(195, 124, 118, .24);
  color: #f5ddda;
}
.cx-message-slide-enter-active {
  transition: all 0.3s ease;
}
.cx-message-slide-leave-active {
  transition: all 0.2s ease;
}
.cx-message-slide-enter-from {
  opacity: 0;
  transform: translateY(-16px) scale(0.94);
}
.cx-message-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.96);
}
</style>
