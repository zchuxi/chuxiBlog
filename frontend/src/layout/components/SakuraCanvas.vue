<template>
  <canvas
    v-show="settings.sakuraEnabled"
    ref="canvasRef"
    class="sakura-canvas"
  ></canvas>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch, nextTick } from 'vue'
import { useSettingsStore } from '../../stores/settings'
import sakuraImg from '../../assets/sakura.png'

const settings = useSettingsStore()
const canvasRef = ref(null)
let sakuraRaf = null
let petals = []
const petalImg = new Image()
petalImg.src = sakuraImg

function spawnPetal(canvas, anywhere) {
  return {
    x: Math.random() * canvas.width,
    y: anywhere ? Math.random() * canvas.height : -30,
    vx: -0.4 + Math.random() * 0.8,
    vy: 0.6 + Math.random() * 1.4,
    vr: (Math.random() - 0.5) * 0.03,
    rot: Math.random() * Math.PI * 2,
    size: 14 + Math.random() * 18,
    alpha: 0.5 + Math.random() * 0.5,
    phase: Math.random() * Math.PI * 2,
    sway: 0.01 + Math.random() * 0.02
  }
}

function startSakura() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  canvas.width = window.innerWidth
  canvas.height = window.innerHeight
  if (!petals.length) {
    petals = Array.from({ length: 24 }, () => spawnPetal(canvas, true))
  }
  const tick = () => {
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    for (const p of petals) {
      p.y += p.vy
      p.x += p.vx + Math.sin(p.phase += p.sway) * 0.6
      p.rot += p.vr
      if (p.y > canvas.height + 40) Object.assign(p, spawnPetal(canvas, false))
      ctx.save()
      ctx.translate(p.x, p.y)
      ctx.rotate(p.rot)
      ctx.globalAlpha = p.alpha
      ctx.drawImage(petalImg, -p.size / 2, -p.size / 2, p.size, p.size)
      ctx.restore()
    }
    sakuraRaf = requestAnimationFrame(tick)
  }
  cancelAnimationFrame(sakuraRaf)
  tick()
}

function stopSakura() {
  cancelAnimationFrame(sakuraRaf)
  sakuraRaf = null
}

function onResize() {
  if (settings.sakuraEnabled) startSakura()
}

watch(() => settings.sakuraEnabled, val => {
  if (val) nextTick(startSakura)
  else stopSakura()
})

onMounted(() => {
  window.addEventListener('resize', onResize)
  if (settings.sakuraEnabled) nextTick(startSakura)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  stopSakura()
})
</script>

<style scoped>
.sakura-canvas {
  position: fixed;
  inset: 0;
  width: 100vw;
  height: 100vh;
  pointer-events: none;
  z-index: 60;
}
</style>
