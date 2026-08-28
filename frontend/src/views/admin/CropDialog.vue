<template>
  <teleport to="body">
    <div class="crop-overlay">
      <div class="crop-dialog">
        <header class="crop-head">
          <h3>裁切 · {{ item.name }}</h3>
          <button class="admin-drawer-close" type="button" @click="emit('close')">×</button>
        </header>

        <div class="crop-stage">
          <div v-show="ready" class="crop-canvas" :style="{ width: disp.w + 'px', height: disp.h + 'px' }">
            <img
              ref="imgEl"
              class="crop-img"
              :src="item.url"
              alt=""
              draggable="false"
              @load="onImgLoad"
              @error="loadError = true"
            />
            <!-- 裁切框：box-shadow 压暗框外区域 -->
            <div class="crop-box" :style="boxStyle" @pointerdown.prevent="startDrag('move', $event)">
              <span
                v-for="d in DIRS"
                :key="d"
                class="crop-handle"
                :class="'crop-h-' + d"
                @pointerdown.stop.prevent="startDrag(d, $event)"
              ></span>
            </div>
          </div>
          <p v-if="!ready" class="admin-state">{{ loadError ? '图片加载失败' : '图片加载中…' }}</p>
        </div>

        <footer class="crop-foot">
          <button
            v-for="r in ratioOptions"
            :key="r.label"
            type="button"
            class="crop-ratio-btn"
            :class="{ active: ratioVal === r.value }"
            @click="setRatio(r.value)"
          >
            {{ r.label }}
          </button>
          <span class="crop-format">输出：{{ outExt }} · {{ outPixelText }}</span>
          <CxButton plain @click="emit('close')">取消</CxButton>
          <CxButton
            plain
            :disabled="!ready || saving || !canOverwrite"
            :title="canOverwrite ? '用裁切结果替换原文件，地址不变' : `${srcExt} 会被转成 ${outExt}，格式变了不能覆盖`"
            @click="askOverwrite"
          >覆盖原图</CxButton>
          <CxButton :disabled="!ready || saving" @click="save('new')">
            {{ saving ? '保存中…' : '保存为新图' }}
          </CxButton>
        </footer>
      </div>
    </div>

    <!-- 覆盖是不可撤销的，先确认再动手；提示层盖在裁切弹窗之上，裁切结果一直看得见 -->
    <transition name="admin-fade">
      <div v-if="confirming" class="admin-confirm-mask" @click.self="confirming = false">
        <div class="admin-confirm-box" role="alertdialog" aria-modal="true" aria-label="覆盖原图确认">
          <p class="admin-confirm-text">
            将用裁切结果覆盖原图「{{ item.name }}」，原图不可恢复，确定吗？
          </p>
          <div class="admin-confirm-actions">
            <CxButton plain @click="confirming = false">取消</CxButton>
            <CxButton :disabled="saving" @click="save('overwrite')">
              {{ saving ? '覆盖中…' : '确认覆盖' }}
            </CxButton>
          </div>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup>
import { computed, inject, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { mediaApi } from '../../api/admin'
import CxButton from '../../components/cx/CxButton.vue'
import { buildCropFileName } from './adminUi'

const props = defineProps({
  item: { type: Object, required: true }, // { name, url }
  // 预设裁切比例（宽/高），如封面 2/3；0 表示自由裁切
  ratio: { type: Number, default: 0 }
})
const emit = defineEmits(['close', 'saved'])

const toast = inject('adminToast', msg => alert(msg))

const DIRS = ['nw', 'n', 'ne', 'e', 'se', 's', 'sw', 'w']
const RATIOS = [
  { label: '自由', value: 0 },
  { label: '1:1', value: 1 },
  { label: '4:3', value: 4 / 3 },
  { label: '16:9', value: 16 / 9 },
  { label: '3:4', value: 3 / 4 }
]
// 常见比例标签表：传入的预设比例不在内置列表时（如番剧封面 2:3、HERO 16:10），补一个对应按钮
const RATIO_LABELS = [
  [1, '1:1'], [4 / 3, '4:3'], [3 / 2, '3:2'], [2, '2:1'], [16 / 9, '16:9'], [16 / 10, '16:10'],
  [2 / 3, '2:3'], [3 / 4, '3:4'], [9 / 16, '9:16'], [10 / 16, '10:16']
]
const MIN = 24 // 裁切框最小边长（显示像素）

const imgEl = ref(null)
const ready = ref(false)
const loadError = ref(false)
const saving = ref(false)
const confirming = ref(false) // 覆盖原图的二次确认层
const ratioVal = ref(props.ratio || 0)
const disp = reactive({ w: 0, h: 0 }) // 图片显示尺寸
const box = reactive({ x: 0, y: 0, w: 0, h: 0 }) // 裁切框（显示坐标系）

const boxStyle = computed(() => ({
  left: box.x + 'px',
  top: box.y + 'px',
  width: box.w + 'px',
  height: box.h + 'px'
}))

// 预设比例不在内置列表时，在「自由」后插入对应按钮，保证可见可切换
const ratioOptions = computed(() => {
  const r = props.ratio
  if (!r || RATIOS.some(o => Math.abs(o.value - r) < 1e-6)) return RATIOS
  const hit = RATIO_LABELS.find(([v]) => Math.abs(v - r) < 1e-6)
  return [RATIOS[0], { label: hit ? hit[1] : '预设', value: r }, ...RATIOS.slice(1)]
})

// 输出格式跟随原图：png/webp 保留，其余统一 jpeg
const srcExt = computed(() => (props.item.name.split('.').pop() || '').toLowerCase())
const outType = computed(() => {
  if (srcExt.value === 'png') return 'image/png'
  if (srcExt.value === 'webp') return 'image/webp'
  return 'image/jpeg'
})
const outExt = computed(() => outType.value.replace('image/', ''))

// 覆盖要求扩展名和内容格式仍然一致，所以只有原样输出的三种能覆盖；
// gif/bmp 会被转成 jpeg，扩展名与内容对不上，只能另存为新图
const canOverwrite = computed(() => ['png', 'webp', 'jpg', 'jpeg'].includes(srcExt.value))

// 显示坐标 → 原图坐标的比例（与 devicePixelRatio 无关）
const scaleX = computed(() => (disp.w ? imgEl.value.naturalWidth / disp.w : 1))
const scaleY = computed(() => (disp.h ? imgEl.value.naturalHeight / disp.h : 1))
const outPixelText = computed(() => {
  if (!ready.value) return ''
  return Math.max(1, Math.round(box.w * scaleX.value)) + '×' + Math.max(1, Math.round(box.h * scaleY.value))
})

function clamp(v, min, max) {
  return Math.min(Math.max(v, min), max)
}

function onImgLoad() {
  const img = imgEl.value
  if (!img || !img.naturalWidth) return
  // 按弹窗可用空间等比缩放展示
  const maxW = Math.min(window.innerWidth * 0.82, 760)
  const maxH = Math.min(window.innerHeight * 0.58, 480)
  const scale = Math.min(maxW / img.naturalWidth, maxH / img.naturalHeight, 1)
  disp.w = Math.max(1, Math.round(img.naturalWidth * scale))
  disp.h = Math.max(1, Math.round(img.naturalHeight * scale))
  initBox()
  ready.value = true
}

// 初始框：居中 80%，若有比例则按比例内切
function initBox() {
  let w = disp.w * 0.8
  let h = disp.h * 0.8
  if (ratioVal.value) {
    h = w / ratioVal.value
    if (h > disp.h * 0.8) {
      h = disp.h * 0.8
      w = h * ratioVal.value
    }
  }
  box.w = Math.max(w, MIN)
  box.h = Math.max(h, MIN)
  box.x = (disp.w - box.w) / 2
  box.y = (disp.h - box.h) / 2
}

function setRatio(r) {
  ratioVal.value = r
  if (!r || !ready.value) return
  // 以当前框中心为基准，重设为该比例下能放下的框
  let w = box.w
  let h = w / r
  if (h > box.h) {
    h = box.h
    w = h * r
  }
  if (w > disp.w) {
    w = disp.w
    h = w / r
  }
  if (h > disp.h) {
    h = disp.h
    w = h * r
  }
  w = Math.max(w, MIN)
  h = Math.max(h, MIN)
  const cx = box.x + box.w / 2
  const cy = box.y + box.h / 2
  box.w = w
  box.h = h
  box.x = clamp(cx - w / 2, 0, disp.w - w)
  box.y = clamp(cy - h / 2, 0, disp.h - h)
}

// ---- 拖动 / 缩放 ----
let drag = null // { mode, px, py, x, y, w, h }

function startDrag(mode, e) {
  if (!ready.value) return
  drag = { mode, px: e.clientX, py: e.clientY, x: box.x, y: box.y, w: box.w, h: box.h }
  window.addEventListener('pointermove', onDragMove)
  window.addEventListener('pointerup', endDrag)
}

function onDragMove(e) {
  if (!drag) return
  const dx = e.clientX - drag.px
  const dy = e.clientY - drag.py
  if (drag.mode === 'move') {
    box.x = clamp(drag.x + dx, 0, disp.w - drag.w)
    box.y = clamp(drag.y + dy, 0, disp.h - drag.h)
    return
  }
  applyResize(drag.mode, dx, dy)
}

function applyResize(m, dx, dy) {
  const s = drag
  const r = ratioVal.value
  let left = s.x
  let top = s.y
  let right = s.x + s.w
  let bottom = s.y + s.h
  if (m.includes('e')) right = clamp(right + dx, left + MIN, disp.w)
  if (m.includes('w')) left = clamp(left + dx, 0, right - MIN)
  if (m.includes('s')) bottom = clamp(bottom + dy, top + MIN, disp.h)
  if (m.includes('n')) top = clamp(top + dy, 0, bottom - MIN)

  if (!r) {
    box.x = left
    box.y = top
    box.w = right - left
    box.h = bottom - top
    return
  }

  // 锁比例：以拖动方向的对侧角/边为锚点回算另一维
  const anchorX = m.includes('w') ? right : left
  const anchorY = m.includes('n') ? bottom : top
  const maxW = m.includes('w') ? anchorX : disp.w - anchorX
  const maxH = m.includes('n') ? anchorY : disp.h - anchorY
  let w = right - left
  let h = bottom - top
  if (m === 'n' || m === 's') w = h * r
  else h = w / r
  if (w > maxW) {
    w = maxW
    h = w / r
  }
  if (h > maxH) {
    h = maxH
    w = h * r
  }
  w = Math.max(w, MIN)
  h = Math.max(h, MIN)
  box.w = w
  box.h = h
  box.x = m.includes('w') ? anchorX - w : anchorX
  box.y = m.includes('n') ? anchorY - h : anchorY
}

function endDrag() {
  drag = null
  window.removeEventListener('pointermove', onDragMove)
  window.removeEventListener('pointerup', endDrag)
}

onBeforeUnmount(endDrag)

function onDocumentKeydown(event) {
  const isSaveShortcut = (event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 's'
  if (event.key !== 'Escape' && !isSaveShortcut) return
  event.preventDefault()
  event.stopPropagation()
  if (event.key !== 'Escape' || saving.value) return
  // 确认层开着时 Esc 只收起提示，不把整个裁切弹窗一起关掉
  if (confirming.value) confirming.value = false
  else emit('close')
}

onMounted(() => document.addEventListener('keydown', onDocumentKeydown))
onBeforeUnmount(() => document.removeEventListener('keydown', onDocumentKeydown))

// ---- 保存：按原图分辨率裁出，另存为新图或覆盖原图 ----

const EXT_MAP = { 'image/png': '.png', 'image/webp': '.webp', 'image/jpeg': '.jpg' }

function askOverwrite() {
  if (!ready.value || saving.value || !canOverwrite.value) return
  confirming.value = true
}

async function save(mode) {
  const img = imgEl.value
  if (!img || saving.value) return
  const sx = Math.round(box.x * scaleX.value)
  const sy = Math.round(box.y * scaleY.value)
  const sw = Math.max(1, Math.round(box.w * scaleX.value))
  const sh = Math.max(1, Math.round(box.h * scaleY.value))
  const canvas = document.createElement('canvas')
  canvas.width = sw
  canvas.height = sh
  const ctx = canvas.getContext('2d')
  ctx.drawImage(img, sx, sy, sw, sh, 0, 0, sw, sh)

  saving.value = true
  try {
    const blob = await new Promise(resolve =>
      canvas.toBlob(resolve, outType.value, outType.value === 'image/jpeg' ? 0.92 : undefined)
    )
    if (!blob) throw new Error('裁切失败，浏览器不支持该格式')
    const data = mode === 'overwrite'
      ? await mediaApi.replace(props.item.name, blob)
      : await mediaApi.upload(blob, buildCropFileName(props.item.name, sw, sh, EXT_MAP[outType.value]))
    // 带出新图信息，调用方可直接回填表单字段（覆盖时 url 带 ?v= 版本号，绕开强缓存）
    emit('saved', data, mode === 'overwrite')
    emit('close')
  } catch (err) {
    toast((err && err.message) || '保存失败', 'error')
  } finally {
    saving.value = false
    confirming.value = false
  }
}
</script>
