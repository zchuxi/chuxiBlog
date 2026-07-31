<template>
  <Teleport to="body">
    <div class="music-bottom-bar-shell" :class="{ 'is-hidden': !musicBarOpen }">
      <footer class="music-bottom-bar">
        <div class="music-bar-track">
          <div class="cover-wrap" :class="{ 'is-rotating': playing }">
            <img v-if="currentTrack" class="cover-image" :src="currentTrack.coverUrl" alt="音乐封面" />
            <div v-else class="cover-fallback"><SvgIcon name="common-music" size="18px" /></div>
          </div>
          <div class="meta-wrap">
            <div class="song-name">{{ currentTrack ? currentTrack.title : '暂无歌曲' }}</div>
            <div class="song-extra">{{ currentTrack ? `${currentTrack.artist}&${currentTrack.album}` : '' }}</div>
          </div>
        </div>
        <div class="music-bar-controls">
          <button type="button" class="control-btn" @click="prevTrack">
            <SvgIcon name="music-pre" size="16px" />
          </button>
          <button type="button" class="control-btn is-play" @click="togglePlay">
            <SvgIcon :name="playing ? 'music-pause' : 'music-play'" size="18px" />
          </button>
          <button type="button" class="control-btn" @click="nextTrack">
            <SvgIcon name="music-next" size="16px" />
          </button>
          <button type="button" class="control-btn" title="停止" @click="stopTrack">
            <span class="music-stop-square"></span>
          </button>
        </div>
        <span class="music-time-tag">{{ formatTime(currentTime) }}</span>
        <input
          class="music-bar-progress"
          type="range"
          min="0"
          :max="duration || 0"
          step="0.1"
          :value="currentTime"
          @input="onSeek"
        />
        <span class="music-time-tag">{{ formatTime(duration) }}</span>
        <div class="music-bar-volume">
          <button type="button" class="control-btn" :title="muted ? '取消静音' : '静音'" @click="toggleMute">
            <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M11 5 6 9H3v6h3l5 4V5z" fill="currentColor" stroke="none" />
              <template v-if="!muted">
                <path d="M15.5 8.5a5 5 0 0 1 0 7" />
                <path d="M18.2 6a8.6 8.6 0 0 1 0 12" />
              </template>
              <template v-else>
                <line x1="16" y1="9.5" x2="21" y2="14.5" />
                <line x1="21" y1="9.5" x2="16" y2="14.5" />
              </template>
            </svg>
          </button>
          <input
            class="music-volume-slider"
            type="range"
            min="0"
            max="1"
            step="0.01"
            :value="muted ? 0 : volume"
            @input="onVolumeInput"
          />
        </div>
        <button type="button" class="control-btn" title="播放模式" @click="cyclePlayMode">
          <SvgIcon :name="playModeIcon" size="16px" />
        </button>
        <button type="button" class="control-btn music-rate-btn" title="播放速度" @click="cycleRate">{{ rateLabel }}</button>
        <div class="cx-popover-wrapper">
          <div class="cx-popover-trigger">
            <button type="button" class="control-btn" @click="playlistOpen = !playlistOpen">
              <SvgIcon name="music-list" size="18px" />
            </button>
          </div>
          <transition name="cx-popover-fade">
            <div v-if="playlistOpen" class="cx-popover music-playlist-popover">
              <div
                v-for="(t, i) in tracks"
                :key="t.id"
                class="cx-popover-item"
                @click="playIndex(i)"
              >
                <span class="cx-popover-item__content">{{ t.title }} - {{ t.artist }}</span>
              </div>
            </div>
          </transition>
        </div>
        <button type="button" class="control-btn bottom-bar-close-btn" @click="musicBarOpen = false">
          <SvgIcon name="common-big-close" size="16px" />
        </button>
        <audio
          ref="audioRef"
          :src="currentTrack ? currentTrack.musicUrl : ''"
          @timeupdate="onTimeUpdate"
          @loadedmetadata="onLoadedMeta"
          @ended="nextTrack"
        ></audio>
      </footer>
    </div>
  </Teleport>
</template>

<script setup>
import { computed, ref, nextTick } from 'vue'
import SvgIcon from '../../components/SvgIcon.vue'
import { api } from '../../api'

const props = defineProps({
  musicBarOpen: { type: Boolean, default: false }
})

const emit = defineEmits(['update:musicBarOpen'])

const musicBarOpen = computed({
  get: () => props.musicBarOpen,
  set: (val) => emit('update:musicBarOpen', val)
})

const playlistOpen = ref(false)
const tracks = ref([])
const trackIndex = ref(0)
const playing = ref(false)
const currentTime = ref(0)
const duration = ref(0)
const playMode = ref('order')
const audioRef = ref(null)
const volume = ref(0.8)
const muted = ref(false)
const playbackRate = ref(1)
const RATES = [1, 1.25, 1.5, 2, 0.75]

const currentTrack = computed(() => tracks.value[trackIndex.value] || null)
const playModeIcon = computed(() => ({ order: 'music-order', shuffle: 'music-shuffle', repeatOne: 'music-repeatOne' }[playMode.value]))
const rateLabel = computed(() => `${playbackRate.value}x`)

async function toggleMusicBar() {
  musicBarOpen.value = !musicBarOpen.value
  if (musicBarOpen.value && !tracks.value.length) {
    try {
      const data = await api.music()
      tracks.value = data.records || []
    } catch (e) { console.warn('[音乐] 加载失败:', e) }
  }
}

function togglePlay() {
  const audio = audioRef.value
  if (!audio || !currentTrack.value) return
  if (playing.value) { audio.pause(); playing.value = false }
  else { audio.play().then(() => { playing.value = true }).catch(() => {}) }
}

function playIndex(i) {
  trackIndex.value = i
  playlistOpen.value = false
  nextTick(() => {
    const audio = audioRef.value
    if (audio) audio.play().then(() => { playing.value = true }).catch(() => {})
  })
}

function prevTrack() { playIndex((trackIndex.value - 1 + tracks.value.length) % Math.max(tracks.value.length, 1)) }
function nextTrack() {
  if (playMode.value === 'repeatOne') { playIndex(trackIndex.value); return }
  if (playMode.value === 'shuffle') { playIndex(Math.floor(Math.random() * tracks.value.length)); return }
  playIndex((trackIndex.value + 1) % Math.max(tracks.value.length, 1))
}
function cyclePlayMode() {
  playMode.value = { order: 'shuffle', shuffle: 'repeatOne', repeatOne: 'order' }[playMode.value]
}

function applyAudioPrefs() {
  const audio = audioRef.value
  if (!audio) return
  audio.volume = muted.value ? 0 : volume.value
  audio.playbackRate = playbackRate.value
}

function onVolumeInput(e) {
  volume.value = Number(e.target.value)
  muted.value = volume.value === 0
  applyAudioPrefs()
}

function toggleMute() {
  muted.value = !muted.value
  applyAudioPrefs()
}

function cycleRate() {
  playbackRate.value = RATES[(RATES.indexOf(playbackRate.value) + 1) % RATES.length]
  applyAudioPrefs()
}

function stopTrack() {
  const audio = audioRef.value
  if (!audio) return
  audio.pause()
  audio.currentTime = 0
  playing.value = false
  currentTime.value = 0
}

function onSeek(e) {
  const audio = audioRef.value
  if (audio) audio.currentTime = Number(e.target.value)
}

function onTimeUpdate() { currentTime.value = audioRef.value ? audioRef.value.currentTime : 0 }
function onLoadedMeta() {
  duration.value = audioRef.value ? audioRef.value.duration : 0
  applyAudioPrefs()
}

function formatTime(t) {
  if (!Number.isFinite(t)) return '00:00'
  const m = Math.floor(t / 60), s = Math.floor(t % 60)
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

defineExpose({ toggleMusicBar })
</script>
