<template>
  <div ref="rootRef" class="cx-date-picker" :class="{ 'is-disabled': disabled, 'is-open': open }">
    <button
      type="button"
      class="cx-date-picker__field"
      :id="id"
      :name="name"
      :aria-invalid="ariaInvalid"
      :aria-describedby="ariaDescribedby"
      :aria-required="ariaRequired"
      :disabled="disabled"
      :aria-expanded="open ? 'true' : 'false'"
      aria-haspopup="dialog"
      @click="toggle"
    >
      <span v-if="displayText" class="cx-date-picker__value">{{ displayText }}</span>
      <span v-else class="cx-date-picker__placeholder">{{ placeholder || (withTime ? '选择日期时间' : '选择日期') }}</span>
      <SvgIcon name="common-history" size="15px" class="cx-date-picker__icon" />
    </button>

    <!-- 面板 teleport 到 body：作为独立弹层展示，避免嵌套在原组件内被表单/弹窗裁剪 -->
    <Teleport to="body">
    <transition name="cx-date-picker-fade">
      <div
        v-if="open"
        ref="panelRef"
        class="cx-date-picker__panel"
        :style="panelStyle"
        role="dialog"
        :aria-label="withTime ? '选择日期时间' : '选择日期'"
      >
        <div class="cx-date-picker__body">
          <!-- 日历 -->
          <div class="cx-date-picker__calendar">
            <div class="cx-date-picker__head">
              <span class="cx-date-picker__month">{{ cursorLabel }}</span>
              <div class="cx-date-picker__nav">
                <button type="button" aria-label="上一月" @click="shiftMonth(-1)">‹</button>
                <button type="button" aria-label="下一月" @click="shiftMonth(1)">›</button>
              </div>
            </div>
            <div class="cx-date-picker__week">
              <span v-for="w in WEEK_LABELS" :key="w">{{ w }}</span>
            </div>
            <div class="cx-date-picker__grid">
              <button
                v-for="d in days"
                :key="d.key"
                type="button"
                class="cx-date-picker__day"
                :class="{ 'is-outside': d.outside, 'is-today': d.today, 'is-active': d.active }"
                @click="pickDate(d.date)"
              >{{ d.label }}</button>
            </div>
          </div>

          <!-- 时分秒 -->
          <div v-if="withTime" class="cx-date-picker__time">
            <div class="cx-date-picker__time-col" role="listbox" aria-label="时">
              <button
                v-for="h in HOURS" :key="'h' + h" type="button"
                :class="{ 'is-active': h === timeParts.h }"
                @click="pickTime('h', h)"
              >{{ pad(h) }}</button>
            </div>
            <div class="cx-date-picker__time-col" role="listbox" aria-label="分">
              <button
                v-for="m in MINUTES" :key="'m' + m" type="button"
                :class="{ 'is-active': m === timeParts.m }"
                @click="pickTime('m', m)"
              >{{ pad(m) }}</button>
            </div>
            <div class="cx-date-picker__time-col" role="listbox" aria-label="秒">
              <button
                v-for="s in SECONDS" :key="'s' + s" type="button"
                :class="{ 'is-active': s === timeParts.s }"
                @click="pickTime('s', s)"
              >{{ pad(s) }}</button>
            </div>
          </div>
        </div>

        <div class="cx-date-picker__foot">
          <button type="button" class="cx-date-picker__link" @click="clear">清除</button>
          <div class="cx-date-picker__foot-right">
            <button type="button" class="cx-date-picker__link" @click="pickToday">此刻</button>
            <button type="button" class="cx-date-picker__confirm" @click="open = false">确定</button>
          </div>
        </div>
      </div>
    </transition>
    </Teleport>
  </div>
</template>

<script setup>
/**
 * CX 日期/时间选择器：替代原生 <input type="date|datetime-local">。
 *
 * 原生控件的日历弹层由浏览器绘制，站点主题与暗色模式管不到它，
 * 且弹层尺寸/位置不可控（在管理端弹窗里会溢出并盖住相邻字段）。
 * 本组件自绘弹层，取站点令牌，明暗自动跟随。
 *
 * 值格式与后端契约保持一致，与原先 FieldInput 的 dateValue 相同：
 *   type="date"     → "YYYY-MM-DD"
 *   type="datetime" → "YYYY-MM-DDTHH:mm:ss"
 * 全程按本地时间拼字符串，不经 toISOString()（那会转成 UTC，
 * 在 UTC+8 下会把 00:00~07:59 的日期整体退一天）。
 */
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import SvgIcon from '../SvgIcon.vue'
import { buildMonthGrid, displayDateValue, formatDateValue, pad, parseDateValue } from '../../utils/dateValue.js'

const props = defineProps({
  modelValue: { type: String, default: '' },
  type: { type: String, default: 'datetime' },   // 'date' | 'datetime'
  disabled: Boolean,
  placeholder: { type: String, default: '' },
  id: { type: String, default: undefined },
  name: { type: String, default: undefined },
  ariaInvalid: { type: [Boolean, String], default: undefined },
  ariaDescribedby: { type: String, default: undefined },
  ariaRequired: { type: [Boolean, String], default: undefined }
})
const emit = defineEmits(['update:modelValue'])

const withTime = computed(() => props.type !== 'date')
const WEEK_LABELS = ['一', '二', '三', '四', '五', '六', '日']

const open = ref(false)
const rootRef = ref(null)
const panelRef = ref(null)
const panelStyle = ref({})

/* 解析 / 格式化 / 日格逻辑在 utils/dateValue.js，由 node --test 覆盖 */
const format = dt => formatDateValue(dt, withTime.value)
const selected = computed(() => parseDateValue(props.modelValue))
const displayText = computed(() => displayDateValue(props.modelValue, withTime.value))

/* ---------- 面板浏览到的年月（与选中值解耦，翻月不改值） ---------- */
const cursor = ref(new Date())
watch(() => props.modelValue, () => {
  const d = selected.value
  if (d) cursor.value = new Date(d.getFullYear(), d.getMonth(), 1)
}, { immediate: true })

const cursorLabel = computed(() => `${cursor.value.getFullYear()}年${pad(cursor.value.getMonth() + 1)}月`)

function shiftMonth(delta) {
  cursor.value = new Date(cursor.value.getFullYear(), cursor.value.getMonth() + delta, 1)
}

/* 6×7 日格：补齐上下月，周一为首列 */
const days = computed(() => buildMonthGrid(cursor.value, selected.value))

/* ---------- 时间列 ---------- */
const HOURS = Array.from({ length: 24 }, (_, i) => i)
const MINUTES = Array.from({ length: 60 }, (_, i) => i)
const SECONDS = Array.from({ length: 60 }, (_, i) => i)
const timeParts = computed(() => {
  const d = selected.value
  return { h: d ? d.getHours() : 0, m: d ? d.getMinutes() : 0, s: d ? d.getSeconds() : 0 }
})

/* ---------- 提交 ---------- */
// 选日期时保留已选的时分秒；无值时落到 00:00:00
function pickDate(d) {
  const t = timeParts.value
  emit('update:modelValue', format(new Date(d.getFullYear(), d.getMonth(), d.getDate(), t.h, t.m, t.s)))
  // 点补位格（上下月）时把面板翻到那个月，否则选中项立刻移出视野
  if (d.getMonth() !== cursor.value.getMonth() || d.getFullYear() !== cursor.value.getFullYear()) {
    cursor.value = new Date(d.getFullYear(), d.getMonth(), 1)
  }
  if (!withTime.value) open.value = false
}

function pickTime(unit, val) {
  const d = selected.value || new Date(cursor.value.getFullYear(), cursor.value.getMonth(), cursor.value.getDate())
  const next = new Date(d)
  if (unit === 'h') next.setHours(val)
  else if (unit === 'm') next.setMinutes(val)
  else next.setSeconds(val)
  emit('update:modelValue', format(next))
}

function pickToday() {
  const now = new Date()
  emit('update:modelValue', format(withTime.value ? now : new Date(now.getFullYear(), now.getMonth(), now.getDate())))
  if (!withTime.value) open.value = false
}

function clear() {
  emit('update:modelValue', '')
  open.value = false
}

/* ---------- 开关与关闭 ---------- */
// 与顶栏浮层同口径：pointerdown 判定「点在自身之外」，避免被内部元素吞掉 click
function toggle() {
  if (props.disabled) return
  open.value = !open.value
  if (open.value) nextTick(() => {
    positionPanel()
    scrollTimeColumnsIntoView()
  })
}

// 面板作为独立弹层：按触发字段的视口位置定位，下方空间不足时向上翻转
function positionPanel() {
  const root = rootRef.value
  const panel = panelRef.value
  if (!root || !panel) return
  const rootRect = root.getBoundingClientRect()
  const panelRect = panel.getBoundingClientRect()
  const gap = 6
  const spaceBelow = window.innerHeight - rootRect.bottom
  const spaceAbove = rootRect.top
  const openUp = panelRect.height > spaceBelow && spaceAbove > spaceBelow
  panelStyle.value = {
    position: 'fixed',
    left: `${Math.max(8, Math.min(rootRect.left, window.innerWidth - panelRect.width - 8))}px`,
    // top 与 bottom 必须互斥：样式表给未定位态兜底了 top: calc(100% + 8px)，
    // 上翻时若只设 bottom 不清 top，两条同时生效会把面板压扁在视口底部
    // （实测渲染成 30px 高的细条，看起来就是「弹窗下方被截断」）。
    ...(openUp
      ? { top: 'auto', bottom: `${window.innerHeight - rootRect.top + gap}px` }
      : { top: `${rootRect.bottom + gap}px`, bottom: 'auto' })
  }
}

function onDocPointerDown(e) {
  if (!open.value) return
  if (rootRef.value?.contains(e.target)) return
  if (panelRef.value?.contains(e.target)) return
  open.value = false
}
function onDocKeydown(e) {
  if (!open.value) return
  const isSaveShortcut = (e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 's'
  if (e.key === 'Escape' || isSaveShortcut) {
    e.preventDefault()
    e.stopPropagation()
    if (e.key === 'Escape') open.value = false
  }
}

/**
 * 时间列高度对齐日历实测高度。
 * 日历高 = 月份头 + 周头 + 6 行日格 + 间隙，随字号/间距变化，
 * CSS 里写死数值必然与之错开（曾写 226px，实测日历 254px，差 28px）。
 * 取整块日历高度：两者是 flex 兄弟、顶边同高，所以等高才能上下都齐平，
 * 若减掉月份头会导致列底比日历底短一个头高。
 */
function syncTimeColumnHeight() {
  if (!withTime.value || !panelRef.value) return
  const cal = panelRef.value.querySelector('.cx-date-picker__calendar')
  if (!cal) return
  const h = cal.getBoundingClientRect().height
  if (h > 0) panelRef.value.style.setProperty('--cx-dp-col-h', `${Math.round(h)}px`)
}

// 打开时把已选时分秒滚到可见位置，否则 60 行列表停在顶部看不到当前值
function scrollTimeColumnsIntoView() {
  if (!withTime.value || !panelRef.value) return
  syncTimeColumnHeight()
  panelRef.value.querySelectorAll('.cx-date-picker__time-col').forEach(col => {
    const active = col.querySelector('.is-active')
    if (active) col.scrollTop = active.offsetTop - col.clientHeight / 2 + active.clientHeight / 2
  })
}

onMounted(() => {
  document.addEventListener('pointerdown', onDocPointerDown)
  document.addEventListener('keydown', onDocKeydown)
})
onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', onDocPointerDown)
  document.removeEventListener('keydown', onDocKeydown)
})
</script>
