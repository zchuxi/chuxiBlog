<template>
  <div class="app-shell">
    <!-- 主题切换日月过渡 -->
    <div
      class="sun-moon-theme-transition"
      :class="[settings.isDark ? 'is-dark' : 'is-light', { 'is-visible': themeAnimating }]"
    >
      <div class="sun-moon-theme-transition__bg"></div>
      <div class="sun-moon-theme-transition__stars">
        <div
          v-for="(s, i) in stars"
          :key="i"
          class="star"
          :style="{ left: s.left, top: s.top, width: s.size, height: s.size, animationDelay: s.delay }"
        ></div>
      </div>
      <div class="sun-moon-theme-transition__sun celestial"></div>
      <div class="sun-moon-theme-transition__moon celestial"></div>
      <div class="sun-moon-theme-transition__sea"></div>
    </div>

    <!-- 背景 -->
    <div class="app-shell__background has-image has-depth-motion">
      <span
        v-for="(layer, i) in bgLayers"
        :key="i"
        class="app-shell__background-layer"
        :class="{ 'is-active': activeBgLayer === i, 'is-depth-animated': activeBgLayer === i }"
        :style="{ backgroundImage: layer ? `linear-gradient(rgba(255, 255, 255, 0.16), rgba(255, 255, 255, 0.3)), url(${JSON.stringify(layer)})` : 'none' }"
      ></span>
      <span class="app-shell__background-glow app-shell__background-glow--left"></span>
      <span class="app-shell__background-glow app-shell__background-glow--right"></span>
    </div>

    <!-- 顶栏 -->
    <header class="app-shell-top">
      <div class="shell-brand">
        <span title="返回首页" @click="router.push('/index')">初曦的窝</span>
        <nav ref="navRef" class="shell-nav" @mouseleave="hoverPath = ''">
          <span class="nav-indicator" :style="indicatorStyle"></span>
          <span class="nav-underline" :style="underlineStyle"></span>
          <RouterLink
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="nav-link"
            :class="{ 'is-active': isNavActive(item.path) }"
            @mouseenter="hoverPath = item.path"
          >
            <span class="nav-link__content">
              <SvgIcon :name="item.icon" class="nav-link__icon" />
            </span>
            <span class="nav-link__label">{{ item.label }}</span>
          </RouterLink>
        </nav>
        <div class="lx-popover-wrapper">
          <div class="lx-popover-trigger">
            <button type="button" class="shell-action-btn shell-nav-menu" @click="mobileNavOpen = !mobileNavOpen">
              <SvgIcon name="common-menu" class="action-icon" />
            </button>
          </div>
          <transition name="lx-popover-fade">
            <div v-if="mobileNavOpen" class="lx-popover top-nav-mobile-popover">
              <div
                v-for="item in navItems"
                :key="item.path"
                class="lx-popover-item"
                @click="goNav(item.path)"
              >
                <span class="lx-popover-item__icon"><SvgIcon :name="item.icon" size="16px" /></span>
                <span class="lx-popover-item__content">{{ item.label }}</span>
              </div>
            </div>
          </transition>
        </div>
      </div>
      <div class="shell-actions">
        <button type="button" class="shell-action-btn is-search" @click="searchOpen = true">
          <SvgIcon name="common-search" class="action-icon" />
        </button>
        <button type="button" class="shell-action-btn is-theme" @click="toggleTheme">
          <SvgIcon :name="settings.isDark ? 'common-sun' : 'common-moon'" class="action-icon" />
        </button>
        <button type="button" class="shell-action-btn is-ai" @click="aiExpanded = !aiExpanded">
          <SvgIcon name="common-ai" class="action-icon" />
        </button>
        <button type="button" class="shell-action-btn is-music" @click="toggleMusicBar">
          <SvgIcon name="common-music" class="action-icon" />
        </button>
        <div class="shell-action-cat-wrap">
          <button type="button" class="shell-action-btn is-cat" @click="pawOpen = !pawOpen">
            <SvgIcon name="common-cat" class="action-icon" />
          </button>
          <transition name="paw-rope">
            <div v-if="pawOpen" class="paw-rope">
              <div class="paw-rope__sway">
                <span class="paw-rope__line"></span>
                <span class="paw-rope__paw" title="返回顶部" @click="scrollMainToTop">
                  <SvgIcon name="common-paw" size="22px" />
                </span>
              </div>
            </div>
          </transition>
        </div>
        <button type="button" class="shell-action-btn is-setting" @click="settingOpen = true">
          <SvgIcon name="common-setting" class="action-icon" />
        </button>
        <div class="lx-popover-wrapper">
          <div class="lx-popover-trigger">
            <button type="button" class="shell-action-btn is-person" @click="personMenuOpen = !personMenuOpen">
              <SvgIcon name="common-person" class="action-icon" />
            </button>
          </div>
          <transition name="lx-popover-fade">
            <div v-if="personMenuOpen" class="lx-popover login-person-popover">
              <div class="lx-popover-item" @click="openAuthDialog">
                <span class="lx-popover-item__icon"><SvgIcon name="common-person" size="16px" /></span>
                <span class="lx-popover-item__content">登录 / 注册</span>
              </div>
              <div class="lx-popover-item" @click="goAdmin">
                <span class="lx-popover-item__icon"><SvgIcon name="common-setting" size="16px" /></span>
                <span class="lx-popover-item__content">后台管理</span>
              </div>
            </div>
          </transition>
        </div>
      </div>
    </header>

    <!-- 主体：内容列（主区 + 音乐条） + AI 侧栏两个 flex 子列 -->
    <div class="app-shell-body">
      <div class="app-shell-body__content-col">
        <div class="app-shell-main-wrap">
          <section class="app-shell-main">
            <RouterView v-slot="{ Component }">
              <transition name="content-route" mode="out-in">
                <component :is="Component" />
              </transition>
            </RouterView>
          </section>
        </div>

        <!-- 底部音乐条：Teleport 到 body，避免祖先 transform 影响 fixed 定位 -->
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
            <div class="lx-popover-wrapper">
              <div class="lx-popover-trigger">
                <button type="button" class="control-btn" @click="playlistOpen = !playlistOpen">
                  <SvgIcon name="music-list" size="18px" />
                </button>
              </div>
              <transition name="lx-popover-fade">
                <div v-if="playlistOpen" class="lx-popover music-playlist-popover">
                  <div
                    v-for="(t, i) in tracks"
                    :key="t.id"
                    class="lx-popover-item"
                    @click="playIndex(i)"
                  >
                    <span class="lx-popover-item__content">{{ t.title }} - {{ t.artist }}</span>
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
      </div>

      <!-- AI 助手弹窗（右上角悬浮面板） -->
      <Teleport to="body">
        <transition name="ai-chat-modal-fade">
          <div v-if="aiExpanded" class="ai-chat-modal-mask" @click.self="aiExpanded = false">
            <div class="ai-chat-modal" role="dialog" aria-label="AI 助手">
              <div class="ai-chat-toolbar">
                <div class="ai-chat-toolbar__left">
                  <span class="ai-chat-toolbar__spark">✦</span>
                  <span class="ai-chat-toolbar__title">AI 助手</span>
                </div>
                <div class="ai-chat-toolbar__actions">
                  <span class="ai-chat-toolbar__icon-action" title="重新开始" @click="aiMessages = []">
                    <SvgIcon name="common-reset" size="18px" />
                  </span>
                  <span class="ai-chat-toolbar__icon-action" title="关闭" @click="aiExpanded = false">
                    <SvgIcon name="common-big-close" size="18px" />
                  </span>
                </div>
              </div>
              <div class="ai-chat-message-list">
                <!-- 无对话时展示助手问候气泡 -->
                <div v-if="!aiMessages.length" class="ai-chat-message ai-chat-message--assistant">
                  <div class="ai-chat-message__body">
                    <div class="ai-chat-message__content">
                      <p class="ai-chat-message__text">你好呀，我是站点助手 ✦ 试着输入一个关键词，我会帮你在文章里找找看。</p>
                    </div>
                  </div>
                </div>
                <div
                  v-for="(m, i) in aiMessages"
                  :key="i"
                  class="ai-chat-message"
                  :class="`ai-chat-message--${m.role}`"
                >
                  <div class="ai-chat-message__body">
                    <div class="ai-chat-message__content">
                      <p class="ai-chat-message__text">{{ m.content }}</p>
                    </div>
                  </div>
                </div>
              </div>
              <div class="ai-chat-input-wrapper">
                <div class="ai-chat-input-row">
                  <input
                    v-model="aiInput"
                    class="ai-chat-input-row__field"
                    type="text"
                    placeholder="输入消息，Enter 发送"
                    @keydown.enter.exact.prevent="sendAiMessage"
                  />
                  <button type="button" class="ai-chat-input-row__send" @click="sendAiMessage">
                    <SvgIcon name="common-send" size="17px" />
                  </button>
                </div>
              </div>
            </div>
          </div>
        </transition>
      </Teleport>
    </div>

    <!-- live2d 看板娘 -->
    <div
      class="live2d-widget"
      :class="{ 'is-hidden': !settings.live2dEnabled }"
      style="--live2d-bottom-offset: 0px; --live2d-bottom-gap: 8px;"
    >
      <div class="live2d-widget__stage">
        <canvas id="live2d-canvas" width="280" height="280" style="touch-action: none;"></canvas>
      </div>
      <!-- AI 入口仅保留顶栏按钮，看板娘不再挂聊天胶囊 -->
      <div class="live2d-widget__actions">
        <button type="button" class="lx-button lx-button--primary is-round is-circle is-plain" @click="aiExpanded = true">
          <span class="lx-button__content"><SvgIcon name="common-chat" size="18px" /></span>
        </button>
        <button type="button" class="lx-button lx-button--info is-round is-circle is-plain is-disabled" disabled>
          <span class="lx-button__content"><SvgIcon name="common-hanger" size="18px" /></span>
        </button>
        <button type="button" class="lx-button lx-button--warning is-round is-circle is-plain" @click="reloadLive2d">
          <span class="lx-button__content"><SvgIcon name="common-reset" size="18px" /></span>
        </button>
        <button type="button" class="lx-button lx-button--danger is-round is-circle is-plain" @click="settings.update({ live2dEnabled: false })">
          <span class="lx-button__content"><SvgIcon name="common-big-close" size="18px" /></span>
        </button>
      </div>
    </div>

    <!-- 樱花画布 -->
    <canvas
      v-show="settings.sakuraEnabled"
      ref="sakuraRef"
      class="sakura-canvas"
      style="position: fixed; inset: 0; width: 100vw; height: 100vh; pointer-events: none; z-index: 60;"
    ></canvas>

    <!-- 文章搜索浮层 -->
    <transition name="layout-article-search-panel">
      <div v-if="searchOpen" class="layout-article-search-overlay">
        <div class="layout-article-search-overlay-mask" @click="closeSearch"></div>
        <div class="layout-article-search-panel">
          <div class="layout-article-search-header">
            <div class="layout-article-search-input-shell">
              <span class="layout-article-search-input-icon"><SvgIcon name="common-search" size="18px" /></span>
              <input
                ref="searchInputRef"
                v-model="searchKeyword"
                class="layout-article-search-input"
                placeholder="搜索文章标题、摘要或标签..."
                @keydown.enter="doSearch"
              />
              <button v-if="searchKeyword" class="layout-article-search-clear-btn" @click="searchKeyword = ''; searchResult = null">
                <SvgIcon name="common-big-close" size="14px" />
              </button>
            </div>
            <button class="layout-article-search-close-btn" @click="closeSearch">
              <SvgIcon name="common-big-close" size="16px" />
            </button>
          </div>
          <div class="layout-article-search-body">
            <div v-if="searchLoading" class="layout-article-search-loading-state">
              <span class="layout-article-search-loading-icon"><SvgIcon name="common-loading" size="22px" /></span>
              <p>正在搜索…</p>
            </div>
            <div v-else-if="searchResult && !searchResult.records.length" class="layout-article-search-empty-state">
              <p class="layout-article-search-empty-title">没有找到相关内容</p>
              <p class="layout-article-search-empty-text">换个关键词再试试吧。</p>
            </div>
            <template v-else-if="searchResult">
              <div class="layout-article-search-result-head">
                <span class="layout-article-search-result-title">搜索结果</span>
                <span class="layout-article-search-result-count">共 {{ searchResult.total }} 篇</span>
              </div>
              <div class="layout-article-search-grid">
                <article
                  v-for="a in searchResult.records"
                  :key="a.id"
                  class="layout-article-search-card"
                  @click="openArticle(a.id)"
                >
                  <div class="layout-article-search-card-frame">
                    <div class="layout-article-search-card-media">
                      <img class="layout-article-search-card-image" :src="coverOf(a)" :alt="a.title" />
                    </div>
                    <div class="layout-article-search-card-content">
                      <div class="layout-article-search-card-copy">
                        <div class="layout-article-search-card-meta">
                          <span class="lx-tag lx-tag--primary lx-tag--small is-round">
                            <span class="lx-tag__content"><span class="lx-tag__label">{{ a.categoryName }}</span></span>
                          </span>
                          <span class="layout-article-search-card-date">{{ (a.updatedAt || '').slice(0, 10) }}</span>
                        </div>
                        <h3 class="layout-article-search-card-title">{{ a.title }}</h3>
                        <p class="layout-article-search-card-summary">{{ a.summary }}</p>
                      </div>
                      <div class="layout-article-search-card-footer">
                        <div class="layout-article-search-card-tag-list">
                          <span v-for="t in a.tags" :key="t" class="lx-tag lx-tag--primary lx-tag--small is-round">
                            <span class="lx-tag__content"><span class="lx-tag__prefix">#</span><span class="lx-tag__label">{{ t }}</span></span>
                          </span>
                        </div>
                        <button class="layout-article-search-card-action" type="button">打开全文</button>
                      </div>
                    </div>
                  </div>
                </article>
              </div>
            </template>
            <div v-else class="layout-article-search-empty-state">
              <p class="layout-article-search-empty-title">输入关键词开始搜索</p>
              <p class="layout-article-search-empty-text">支持文章标题、摘要与标签。</p>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- 设置弹窗 -->
    <transition name="setting-dialog-fade">
      <div v-if="settingOpen" class="setting-dialog">
        <div class="setting-dialog__mask" @click="settingOpen = false"></div>
        <div class="setting-dialog__card">
          <button class="setting-dialog__close" @click="settingOpen = false">
            <span class="setting-dialog__close-icon"><SvgIcon name="common-big-close" size="16px" /></span>
          </button>
          <div class="setting-dialog__header">
            <span class="setting-dialog__badge"><SvgIcon name="common-setting" size="16px" /></span>
            <div class="setting-dialog__title-row">
              <h3>站点设置</h3>
              <p>背景、主题与小组件的偏好都在这里。</p>
            </div>
          </div>
          <div class="setting-panel">
            <section class="setting-section">
              <div class="setting-section__title-wrap"><h4>页面特效</h4></div>
              <div class="effect-card">
                <div class="effect-card__content">
                  <span>背景轮播</span>
                  <button
                    class="lx-switch"
                    :class="{ 'is-checked': settings.backgroundCarouselEnabled }"
                    @click="settings.update({ backgroundCarouselEnabled: !settings.backgroundCarouselEnabled })"
                  >
                    <span class="lx-switch__core"><span class="lx-switch__action"></span></span>
                  </button>
                </div>
                <div class="effect-card__content">
                  <span>樱花飘落</span>
                  <button
                    class="lx-switch"
                    :class="{ 'is-checked': settings.sakuraEnabled }"
                    @click="settings.update({ sakuraEnabled: !settings.sakuraEnabled })"
                  >
                    <span class="lx-switch__core"><span class="lx-switch__action"></span></span>
                  </button>
                </div>
                <div class="effect-card__content">
                  <span>看板娘</span>
                  <button
                    class="lx-switch"
                    :class="{ 'is-checked': settings.live2dEnabled }"
                    @click="settings.update({ live2dEnabled: !settings.live2dEnabled })"
                  >
                    <span class="lx-switch__core"><span class="lx-switch__action"></span></span>
                  </button>
                </div>
              </div>
            </section>
            <section class="setting-section">
              <div class="setting-section__title-wrap"><h4>背景图片</h4></div>
              <div class="background-mode-card">
                <div class="background-mode-card__header"><span>横屏背景</span></div>
                <div class="background-mode-card__group gallery-stack">
                  <div class="gallery-strip">
                    <div
                      v-for="img in settings.landscapeImages"
                      :key="img"
                      class="gallery-item"
                      :class="{ 'is-active': settings.selectedLandscapeImage === img }"
                      @click="chooseBackground(img)"
                    >
                      <img :src="img" alt="背景" loading="lazy" />
                      <div class="gallery-item__overlay"></div>
                      <span class="gallery-item__label">{{ img.split('/').pop() }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </section>
          </div>
        </div>
      </div>
    </transition>

    <!-- 登录/注册弹窗（原创设计） -->
    <transition name="login-dialog-fade">
      <div v-if="authOpen" class="login-dialog" role="dialog" aria-modal="true">
        <div class="login-dialog__mask" @click="authOpen = false"></div>
        <div class="login-dialog__card">
          <button type="button" class="login-dialog__close" @click="authOpen = false">
            <span class="login-dialog__close-icon"><SvgIcon name="common-big-close" size="14px" /></span>
          </button>
          <!-- 左侧渐变装饰侧板 -->
          <aside class="login-dialog__side">
            <span class="login-dialog__orb login-dialog__orb--1"></span>
            <span class="login-dialog__orb login-dialog__orb--2"></span>
            <span class="login-dialog__orb login-dialog__orb--3"></span>
            <span class="login-dialog__star login-dialog__star--1">✦</span>
            <span class="login-dialog__star login-dialog__star--2">✧</span>
            <span class="login-dialog__star login-dialog__star--3">✦</span>
            <div class="login-dialog__side-body">
              <span class="login-dialog__paw"><SvgIcon name="common-paw" size="28px" /></span>
              <h3 class="login-dialog__side-title">{{ authPanel === 'login' ? 'Hi，朋友！' : '欢迎加入！' }}</h3>
              <p class="login-dialog__side-desc">{{ authPanel === 'login' ? '登录后可以点赞、评论，和这个小站有更多互动。' : '注册一个账号，把喜欢的内容都收藏起来。' }}</p>
              <button
                type="button"
                class="login-dialog__side-btn"
                @click="authPanel = authPanel === 'login' ? 'register' : 'login'"
              >{{ authPanel === 'login' ? '去注册' : '去登录' }}</button>
            </div>
          </aside>
          <!-- 右侧表单区 -->
          <div class="login-dialog__main">
            <transition name="login-dialog-panel" mode="out-in">
              <!-- 登录面板 -->
              <form v-if="authPanel === 'login'" key="login" class="login-dialog__panel" @submit.prevent>
                <h3 class="login-dialog__title">欢迎回来</h3>
                <p class="login-dialog__subtitle">和这个小站继续昨天的故事吧</p>
                <div class="login-dialog__mode-switch">
                  <button
                    type="button"
                    class="login-dialog__mode-btn"
                    :class="{ 'is-active': authMode === 'password' }"
                    @click="authMode = 'password'"
                  >密码登录</button>
                  <button
                    type="button"
                    class="login-dialog__mode-btn"
                    :class="{ 'is-active': authMode === 'code' }"
                    @click="authMode = 'code'"
                  >验证码登录</button>
                </div>
                <label class="login-dialog__label">邮箱</label>
                <input class="login-dialog__input" type="text" placeholder="请输入邮箱" />
                <template v-if="authMode === 'password'">
                  <label class="login-dialog__label">密码</label>
                  <input class="login-dialog__input" type="password" placeholder="请输入密码" />
                </template>
                <template v-else>
                  <label class="login-dialog__label">验证码</label>
                  <div class="login-dialog__code-row">
                    <input class="login-dialog__input" type="text" placeholder="6 位验证码" />
                    <button type="button" class="login-dialog__code-btn">获取验证码</button>
                  </div>
                </template>
                <div class="login-dialog__aux">
                  <button type="button" class="login-dialog__link">忘记密码？</button>
                </div>
                <button type="button" class="login-dialog__submit" @click="authOpen = false">登 录</button>
                <p class="login-dialog__tip">演示站点：登录能力未开放，仅展示界面。</p>
                <p class="login-dialog__switch-hint">
                  还没有账号？<button type="button" class="login-dialog__link" @click="authPanel = 'register'">去注册</button>
                </p>
              </form>
              <!-- 注册面板 -->
              <form v-else key="register" class="login-dialog__panel" @submit.prevent>
                <h3 class="login-dialog__title">创建账号</h3>
                <p class="login-dialog__subtitle">只差一步，就能把喜欢都收藏起来</p>
                <label class="login-dialog__label">邮箱</label>
                <input class="login-dialog__input" type="text" placeholder="请输入邮箱" />
                <label class="login-dialog__label">验证码</label>
                <div class="login-dialog__code-row">
                  <input class="login-dialog__input" type="text" placeholder="6 位验证码" />
                  <button type="button" class="login-dialog__code-btn">获取验证码</button>
                </div>
                <label class="login-dialog__label">密码</label>
                <input class="login-dialog__input" type="password" placeholder="设置密码（至少 8 位）" />
                <button type="button" class="login-dialog__submit" @click="authOpen = false">注 册</button>
                <p class="login-dialog__tip">演示站点：注册能力未开放，仅展示界面。</p>
                <p class="login-dialog__switch-hint">
                  已有账号？<button type="button" class="login-dialog__link" @click="authPanel = 'login'">去登录</button>
                </p>
              </form>
            </transition>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import SvgIcon from '../components/SvgIcon.vue'
import { useSettingsStore } from '../stores/settings'
import { api } from '../api'
import sakuraImg from '../assets/sakura.png'

const settings = useSettingsStore()
const route = useRoute()
const router = useRouter()

/* ---------- 主题 ---------- */
const themeAnimating = ref(false)
let themeTimer = null
const stars = Array.from({ length: 90 }, () => ({
  left: `${(Math.random() * 100).toFixed(4)}%`,
  top: `${(Math.random() * 60).toFixed(4)}%`,
  size: `${(1 + Math.random() * 2).toFixed(4)}px`,
  delay: `${(Math.random() * 3).toFixed(2)}s`
}))

function toggleTheme() {
  themeAnimating.value = true
  settings.setTheme(settings.isDark ? 'light' : 'dark')
  clearTimeout(themeTimer)
  themeTimer = setTimeout(() => { themeAnimating.value = false }, 1600)
}

/* ---------- 背景 ---------- */
const bgLayers = ref([settings.selectedLandscapeImage, ''])
const activeBgLayer = ref(0)
let bgTimer = null

function isPortrait() {
  return window.innerHeight > window.innerWidth
}

function currentPool() {
  return isPortrait() ? settings.verticalImages : settings.landscapeImages
}

function swapBackground(img) {
  const next = activeBgLayer.value === 0 ? 1 : 0
  bgLayers.value[next] = img
  activeBgLayer.value = next
}

function chooseBackground(img) {
  settings.update(isPortrait() ? { selectedVerticalImage: img } : { selectedLandscapeImage: img })
  swapBackground(img)
}

function startBgCarousel() {
  stopBgCarousel()
  if (!settings.backgroundCarouselEnabled) return
  bgTimer = setInterval(() => {
    const pool = currentPool()
    const cur = bgLayers.value[activeBgLayer.value]
    let next = pool[Math.floor(Math.random() * pool.length)]
    if (next === cur) next = pool[(pool.indexOf(next) + 1) % pool.length]
    swapBackground(next)
  }, 30000)
}

function stopBgCarousel() {
  if (bgTimer) { clearInterval(bgTimer); bgTimer = null }
}

watch(() => settings.backgroundCarouselEnabled, startBgCarousel)

/* ---------- 导航 ---------- */
const navItems = [
  { path: '/index', label: '首页', icon: 'common-home' },
  { path: '/timeline', label: '时间线', icon: 'common-timeline' },
  { path: '/tree-hole', label: '树洞', icon: 'common-tree' },
  { path: '/parallax', label: '视差', icon: 'common-parallax' },
  { path: '/archive', label: '归档', icon: 'common-archive' },
  { path: '/tool', label: '工具', icon: 'common-tool' },
  { path: '/bangumi', label: '番剧', icon: 'common-articlePages' },
  { path: '/about', label: '关于', icon: 'common-person' },
  { path: '/components', label: '组件', icon: 'common-component' }
]
const navRef = ref(null)
const indicatorStyle = ref({})
const underlineStyle = ref({})
const hoverPath = ref('')
const mobileNavOpen = ref(false)

function isNavActive(path) {
  if (path === '/index') return route.path === '/index' || route.path.startsWith('/article')
  // 详情页（/tool/1、/bangumi/1…）保持父级导航高亮
  return route.path === path || route.path.startsWith(`${path}/`)
}

function updateIndicator() {
  const nav = navRef.value
  if (!nav) return
  const active = nav.querySelector('.nav-link.is-active')
  if (!active) { indicatorStyle.value = { opacity: 0 }; updateUnderline(); return }
  const navRect = nav.getBoundingClientRect()
  const rect = active.getBoundingClientRect()
  const cx = rect.left - navRect.left + rect.width / 2
  const cy = rect.top - navRect.top + rect.height / 2
  indicatorStyle.value = {
    transform: `translate(${cx}px, ${cy}px) translate(-50%, -50%)`,
    width: `${rect.width}px`,
    height: `${rect.height}px`,
    opacity: 1
  }
  updateUnderline()
}

/* 下划线：常态停在当前页，悬停时跟随，移开弹回 */
function updateUnderline() {
  const nav = navRef.value
  if (!nav) return
  const target = hoverPath.value
    ? nav.querySelector(`.nav-link[href="${hoverPath.value}"]`)
    : nav.querySelector('.nav-link.is-active')
  if (!target) { underlineStyle.value = { opacity: 0 }; return }
  const navRect = nav.getBoundingClientRect()
  const rect = target.getBoundingClientRect()
  underlineStyle.value = {
    transform: `translateX(${rect.left - navRect.left + rect.width / 2}px) translateX(-50%)`,
    width: `${Math.max(18, rect.width * 0.52)}px`,
    opacity: 1
  }
}

watch(hoverPath, () => nextTick(updateUnderline))
watch(() => route.path, () => nextTick(updateIndicator))

function goNav(path) {
  mobileNavOpen.value = false
  router.push(path)
}

/* ---------- 音乐 ---------- */
const musicBarOpen = ref(false)
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
    } catch { /* 忽略 */ }
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
/** 把音量/倍速同步到 audio（换歌重载后倍速会被重置，需重新应用） */
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

/* ---------- AI 助手弹窗 ---------- */
const aiExpanded = ref(false)
const aiInput = ref('')
const aiMessages = ref([])

// 弹窗打开时支持 Esc 关闭
function onAiKeydown(e) { if (e.key === 'Escape') aiExpanded.value = false }
watch(aiExpanded, open => {
  if (open) document.addEventListener('keydown', onAiKeydown)
  else document.removeEventListener('keydown', onAiKeydown)
})

function sendAiMessage() {
  const text = aiInput.value.trim()
  if (!text) return
  aiMessages.value.push({ role: 'user', content: text })
  aiInput.value = ''
  setTimeout(() => {
    aiMessages.value.push({ role: 'assistant', content: '演示环境：AI 助手暂未接入模型服务，先陪你聊到这里啦。' })
  }, 400)
}

/* ---------- live2d ---------- */
function loadLive2dScript() {
  return new Promise((resolve, reject) => {
    if (window.loadlive2d) { resolve(); return }
    const s = document.createElement('script')
    s.src = '/live2d/live2d.min.js'
    s.onload = resolve
    s.onerror = reject
    document.body.appendChild(s)
  })
}

async function initLive2d() {
  try {
    await loadLive2dScript()
    if (window.loadlive2d) {
      window.loadlive2d('live2d-canvas', '/live2d/model/mashiro/shifuku.model.json')
    }
  } catch { /* live2d 加载失败时静默 */ }
}

function reloadLive2d() {
  if (window.loadlive2d) {
    window.loadlive2d('live2d-canvas', '/live2d/model/mashiro/shifuku.model.json')
  }
}

watch(() => settings.live2dEnabled, val => { if (val) nextTick(initLive2d) })

/* ---------- 樱花 ---------- */
const sakuraRef = ref(null)
const pawOpen = ref(false)
let sakuraRaf = null
let petals = []
const petalImg = new Image()
petalImg.src = sakuraImg

function startSakura() {
  const canvas = sakuraRef.value
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

function stopSakura() {
  cancelAnimationFrame(sakuraRaf)
  sakuraRaf = null
}

watch(() => settings.sakuraEnabled, val => {
  if (val) nextTick(startSakura)
  else stopSakura()
})

// 猫爪吊绳：返回顶部（收起交给滚动监听，回到顶部后自动消失）
function scrollMainToTop() {
  const main = document.querySelector('.app-shell-main')
  if (main) main.scrollTo({ top: 0, behavior: 'smooth' })
}

// 向下滚动自动展开吊绳，回到顶部自动收起
let pawScrollEl = null
function onMainScroll() {
  pawOpen.value = pawScrollEl && pawScrollEl.scrollTop > 120
}
function bindPawScroll() {
  pawScrollEl = document.querySelector('.app-shell-main')
  if (pawScrollEl) pawScrollEl.addEventListener('scroll', onMainScroll, { passive: true })
}

/* ---------- 搜索 ---------- */
const searchOpen = ref(false)
const searchKeyword = ref('')
const searchLoading = ref(false)
const searchResult = ref(null)
const searchInputRef = ref(null)

watch(searchOpen, open => {
  if (open) nextTick(() => searchInputRef.value && searchInputRef.value.focus())
})

async function doSearch() {
  const kw = searchKeyword.value.trim()
  if (!kw) return
  searchLoading.value = true
  try {
    searchResult.value = await api.searchArticles(kw, 1, 12)
  } catch {
    searchResult.value = { records: [], total: 0 }
  } finally {
    searchLoading.value = false
  }
}

function closeSearch() {
  searchOpen.value = false
}

function openArticle(id) {
  closeSearch()
  router.push(`/article/${id}`)
}

const FALLBACK_COVERS = ['/image/bg/Landscape/01.webp', '/image/bg/Landscape/05.webp', '/image/bg/Landscape/08.webp', '/image/bg/Landscape/10.webp', '/image/bg/Landscape/12.webp', '/image/bg/Landscape/13.webp']
function coverOf(a) {
  return a.coverUrl || FALLBACK_COVERS[a.id % FALLBACK_COVERS.length]
}

/* ---------- 弹窗 ---------- */
const settingOpen = ref(false)
const authOpen = ref(false)
const authMode = ref('password')
const authPanel = ref('login')
const personMenuOpen = ref(false)

function openAuthDialog() {
  personMenuOpen.value = false
  authPanel.value = 'login'
  authOpen.value = true
}

function goAdmin() {
  personMenuOpen.value = false
  router.push('/admin')
}

// ESC 关闭登录弹窗与账号菜单
function onAuthKeydown(e) {
  if (e.key !== 'Escape') return
  authOpen.value = false
  personMenuOpen.value = false
}

/* ---------- 生命周期 ---------- */
function onResize() {
  updateIndicator()
  if (settings.sakuraEnabled) startSakura()
}

onMounted(() => {
  document.documentElement.classList.toggle('dark', settings.isDark)
  api.bumpViews().catch(() => {})
  nextTick(updateIndicator)
  nextTick(bindPawScroll)
  window.addEventListener('resize', onResize)
  window.addEventListener('keydown', onAuthKeydown)
  startBgCarousel()
  if (settings.live2dEnabled) initLive2d()
  if (settings.sakuraEnabled) nextTick(startSakura)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  window.removeEventListener('keydown', onAuthKeydown)
  if (pawScrollEl) pawScrollEl.removeEventListener('scroll', onMainScroll)
  stopBgCarousel()
  stopSakura()
  clearTimeout(themeTimer)
})
</script>

<style>
/* ========== 顶栏账号菜单 ========== */
.lx-popover.login-person-popover {
  min-width: 168px;
  margin-top: 46px;
  margin-left: -168px;
}

/* ========== 登录/注册弹窗（原创设计，类名前缀 login-dialog）========== */
.login-dialog {
  position: fixed;
  inset: 0;
  z-index: 260;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.login-dialog__mask {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 20% 10%, rgba(157, 180, 255, 0.28), transparent 55%),
    radial-gradient(circle at 85% 90%, rgba(217, 161, 239, 0.24), transparent 55%),
    rgba(78, 96, 148, 0.32);
  backdrop-filter: blur(10px) saturate(1.15);
  -webkit-backdrop-filter: blur(10px) saturate(1.15);
}

/* 玻璃拟态卡片 */
.login-dialog__card {
  position: relative;
  display: flex;
  width: min(780px, 100%);
  min-height: 500px;
  border-radius: 28px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(24px) saturate(1.2);
  -webkit-backdrop-filter: blur(24px) saturate(1.2);
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 28px 68px rgba(88, 111, 214, 0.28), 0 6px 18px rgba(88, 111, 214, 0.14),
    inset 0 1px 0 rgba(255, 255, 255, 0.85);
}

.login-dialog__close {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 5;
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 999px;
  background: rgba(95, 149, 207, 0.12);
  color: #3f77b5;
  cursor: pointer;
  transition: background 0.22s ease, color 0.22s ease;
}

.login-dialog__close-icon {
  display: inline-flex;
  transition: transform 0.28s ease;
}

.login-dialog__close:hover {
  background: rgba(95, 149, 207, 0.22);
}

.login-dialog__close:hover .login-dialog__close-icon {
  transform: rotate(90deg) scale(1.08);
}

/* ----- 左侧渐变侧板 ----- */
.login-dialog__side {
  position: relative;
  flex-shrink: 0;
  width: 292px;
  padding: 40px 32px;
  display: flex;
  align-items: center;
  overflow: hidden;
  color: #fff;
  background: linear-gradient(165deg, #9ec6ea 0%, #86b3e0 52%, #8fd4dd 100%);
}

.login-dialog__side-body {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 14px;
}

.login-dialog__paw {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.22);
  border: 1px solid rgba(255, 255, 255, 0.45);
  box-shadow: 0 8px 20px rgba(58, 100, 150, 0.25);
  animation: login-dialog-bob 3.2s ease-in-out infinite;
}

.login-dialog__side-title {
  margin: 0;
  font-size: 29px;
  letter-spacing: 2px;
  text-shadow: 0 2px 8px rgba(58, 100, 150, 0.3);
}

.login-dialog__side-desc {
  margin: 0;
  font-size: 15.5px;
  line-height: 1.8;
  opacity: 0.92;
}

.login-dialog__side-btn {
  margin-top: 6px;
  padding: 9px 30px;
  border-radius: 999px;
  border: 1.5px solid rgba(255, 255, 255, 0.75);
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
  font: inherit;
  font-size: 15.5px;
  letter-spacing: 3px;
  cursor: pointer;
  transition: background 0.24s ease, color 0.24s ease, transform 0.24s ease, box-shadow 0.24s ease;
}

.login-dialog__side-btn:hover {
  background: rgba(255, 255, 255, 0.92);
  color: #4a7cb8;
  transform: translateY(-2px);
  box-shadow: 0 10px 22px rgba(58, 100, 150, 0.32);
}

.login-dialog__side-btn:active {
  transform: translateY(0) scale(0.97);
}

/* 侧板浮动圆球与星星装饰 */
.login-dialog__orb {
  position: absolute;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.22);
  pointer-events: none;
}

.login-dialog__orb--1 {
  width: 140px;
  height: 140px;
  top: -46px;
  left: -40px;
  animation: login-dialog-float 7s ease-in-out infinite alternate;
}

.login-dialog__orb--2 {
  width: 90px;
  height: 90px;
  right: -28px;
  top: 38%;
  animation: login-dialog-float 8.5s ease-in-out infinite alternate-reverse;
}

.login-dialog__orb--3 {
  width: 120px;
  height: 120px;
  bottom: -42px;
  left: 24%;
  animation: login-dialog-float 6s ease-in-out infinite alternate;
}

.login-dialog__star {
  position: absolute;
  color: rgba(255, 255, 255, 0.85);
  pointer-events: none;
  animation: login-dialog-twinkle 2.8s ease-in-out infinite;
}

.login-dialog__star--1 { top: 14%; right: 22%; font-size: 16.5px; }
.login-dialog__star--2 { top: 42%; left: 12%; font-size: 13px; animation-delay: 0.9s; }
.login-dialog__star--3 { bottom: 16%; right: 16%; font-size: 14.5px; animation-delay: 1.7s; }

/* ----- 右侧表单区 ----- */
.login-dialog__main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 40px 44px;
}

.login-dialog__panel {
  width: 100%;
  max-width: 330px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
}

.login-dialog__title {
  margin: 0;
  text-align: center;
  font-size: 29px;
  letter-spacing: 3px;
  color: #3d4668;
}

.login-dialog__subtitle {
  margin: 8px 0 20px;
  text-align: center;
  font-size: 14.5px;
  color: #93a0c4;
}

/* 密码 / 验证码模式切换 */
.login-dialog__mode-switch {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 6px;
}

.login-dialog__mode-btn {
  height: 34px;
  border-radius: 12px;
  border: 1.5px solid transparent;
  background: rgba(95, 149, 207, 0.1);
  color: #6b7aa8;
  font: inherit;
  font-size: 14.5px;
  cursor: pointer;
  transition: background 0.22s ease, color 0.22s ease, border-color 0.22s ease,
    transform 0.22s ease, box-shadow 0.22s ease;
}

.login-dialog__mode-btn.is-active {
  background: rgba(255, 255, 255, 0.95);
  border-color: rgba(95, 149, 207, 0.55);
  color: #3f77b5;
  transform: translateY(-1px);
  box-shadow: 0 6px 14px rgba(95, 149, 207, 0.22);
}

.login-dialog__mode-btn:not(.is-active):hover {
  background: rgba(95, 149, 207, 0.18);
  color: #3f77b5;
}

.login-dialog__label {
  margin: 12px 0 6px;
  font-size: 14.5px;
  color: #6b7aa8;
}

.login-dialog__input {
  width: 100%;
  height: 42px;
  padding: 0 14px;
  border-radius: 14px;
  border: 1.5px solid rgba(130, 150, 220, 0.35);
  background: rgba(255, 255, 255, 0.75);
  color: #3d4668;
  font: inherit;
  font-size: 15.5px;
  outline: none;
  transition: border-color 0.24s ease, box-shadow 0.24s ease, transform 0.24s ease,
    background 0.24s ease;
}

.login-dialog__input::placeholder {
  color: #a9b4d4;
}

.login-dialog__input:hover {
  border-color: rgba(95, 149, 207, 0.55);
}

/* 输入框聚焦动效：光环 + 轻微上浮 */
.login-dialog__input:focus {
  border-color: #6d9bd6;
  background: #fff;
  box-shadow: 0 0 0 4px rgba(95, 149, 207, 0.18), 0 6px 16px rgba(95, 149, 207, 0.16);
  transform: translateY(-1px);
}

.login-dialog__code-row {
  display: flex;
  gap: 10px;
}

.login-dialog__code-row .login-dialog__input {
  flex: 1;
  min-width: 0;
}

.login-dialog__code-btn {
  flex-shrink: 0;
  padding: 0 14px;
  border-radius: 14px;
  border: 1.5px solid rgba(95, 149, 207, 0.5);
  background: transparent;
  color: #3f77b5;
  font: inherit;
  font-size: 14.5px;
  cursor: pointer;
  transition: background 0.22s ease, transform 0.22s ease, box-shadow 0.22s ease;
}

.login-dialog__code-btn:hover {
  background: rgba(95, 149, 207, 0.14);
  transform: translateY(-1px);
  box-shadow: 0 6px 14px rgba(95, 149, 207, 0.18);
}

.login-dialog__aux {
  margin-top: 10px;
  text-align: right;
}

.login-dialog__link {
  padding: 0;
  border: none;
  background: none;
  color: #6d9bd6;
  font: inherit;
  font-size: 14.5px;
  cursor: pointer;
  transition: color 0.2s ease;
}

.login-dialog__link:hover {
  color: #3f77b5;
  text-decoration: underline;
}

.login-dialog__submit {
  margin-top: 16px;
  height: 44px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #6d9bd6 0%, #67b7cf 100%);
  color: #fff;
  font: inherit;
  font-size: 16.5px;
  letter-spacing: 6px;
  text-indent: 6px;
  cursor: pointer;
  box-shadow: 0 12px 26px rgba(95, 149, 207, 0.38);
  transition: transform 0.24s ease, box-shadow 0.24s ease, filter 0.24s ease;
}

.login-dialog__submit:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 32px rgba(95, 149, 207, 0.46);
  filter: brightness(1.04);
}

.login-dialog__submit:active {
  transform: translateY(0) scale(0.98);
  box-shadow: 0 8px 18px rgba(95, 149, 207, 0.32);
}

.login-dialog__tip {
  margin: 14px 0 0;
  text-align: center;
  font-size: 13px;
  color: #93a0c4;
}

.login-dialog__switch-hint {
  margin: 8px 0 0;
  text-align: center;
  font-size: 14.5px;
  color: #6b7aa8;
}

/* ----- 弹窗进出场动效 ----- */
.login-dialog-fade-enter-active {
  transition: opacity 0.3s ease;
}

.login-dialog-fade-enter-active .login-dialog__card {
  transition: transform 0.42s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.3s ease;
}

.login-dialog-fade-leave-active {
  transition: opacity 0.22s ease;
}

.login-dialog-fade-leave-active .login-dialog__card {
  transition: transform 0.22s ease, opacity 0.22s ease;
}

.login-dialog-fade-enter-from,
.login-dialog-fade-leave-to {
  opacity: 0;
}

.login-dialog-fade-enter-from .login-dialog__card {
  transform: translateY(26px) scale(0.94);
  opacity: 0;
}

.login-dialog-fade-leave-to .login-dialog__card {
  transform: translateY(12px) scale(0.97);
  opacity: 0;
}

/* 登录/注册面板切换动效 */
.login-dialog-panel-enter-active,
.login-dialog-panel-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}

.login-dialog-panel-enter-from {
  opacity: 0;
  transform: translateX(16px);
}

.login-dialog-panel-leave-to {
  opacity: 0;
  transform: translateX(-16px);
}

/* ----- 装饰动画 ----- */
@keyframes login-dialog-float {
  from { transform: translateY(-8px); }
  to { transform: translateY(10px); }
}

@keyframes login-dialog-bob {
  0%, 100% { transform: translateY(0) rotate(-4deg); }
  50% { transform: translateY(-6px) rotate(6deg); }
}

@keyframes login-dialog-twinkle {
  0%, 100% { opacity: 0.35; transform: scale(0.85); }
  50% { opacity: 1; transform: scale(1.12); }
}

/* ----- 暗色模式 ----- */
html.dark .login-dialog__mask {
  background: radial-gradient(circle at 20% 10%, rgba(70, 120, 180, 0.2), transparent 55%),
    radial-gradient(circle at 85% 90%, rgba(80, 150, 180, 0.16), transparent 55%),
    rgba(10, 14, 28, 0.55);
}

html.dark .login-dialog__card {
  background: rgba(32, 38, 62, 0.9);
  border-color: rgba(255, 255, 255, 0.08);
  box-shadow: 0 28px 68px rgba(0, 0, 0, 0.5), 0 6px 18px rgba(0, 0, 0, 0.35),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);
}

html.dark .login-dialog__close {
  background: rgba(95, 149, 207, 0.16);
  color: #a8cdf0;
}

html.dark .login-dialog__close:hover {
  background: rgba(95, 149, 207, 0.3);
}

html.dark .login-dialog__side {
  background: linear-gradient(165deg, #274a75 0%, #2f5d88 55%, #2f7a8a 100%);
}

html.dark .login-dialog__title {
  color: #e8ecff;
}

html.dark .login-dialog__subtitle,
html.dark .login-dialog__tip {
  color: #8d97bd;
}

html.dark .login-dialog__label,
html.dark .login-dialog__switch-hint {
  color: #aab4d8;
}

html.dark .login-dialog__mode-btn {
  background: rgba(255, 255, 255, 0.06);
  color: #aab4d8;
}

html.dark .login-dialog__mode-btn.is-active {
  background: rgba(95, 149, 207, 0.22);
  border-color: rgba(127, 176, 221, 0.6);
  color: #bcd9f2;
  box-shadow: 0 6px 14px rgba(0, 0, 0, 0.3);
}

html.dark .login-dialog__mode-btn:not(.is-active):hover {
  background: rgba(255, 255, 255, 0.1);
  color: #bcd9f2;
}

html.dark .login-dialog__input {
  border-color: rgba(127, 176, 221, 0.28);
  background: rgba(18, 22, 40, 0.65);
  color: #e8ecff;
}

html.dark .login-dialog__input::placeholder {
  color: #6b7599;
}

html.dark .login-dialog__input:hover {
  border-color: rgba(127, 176, 221, 0.5);
}

html.dark .login-dialog__input:focus {
  border-color: #7fb0dd;
  background: rgba(18, 22, 40, 0.9);
  box-shadow: 0 0 0 4px rgba(127, 176, 221, 0.16), 0 6px 16px rgba(0, 0, 0, 0.3);
}

html.dark .login-dialog__code-btn {
  border-color: rgba(127, 176, 221, 0.45);
  color: #a8cdf0;
}

html.dark .login-dialog__code-btn:hover {
  background: rgba(127, 176, 221, 0.14);
  box-shadow: 0 6px 14px rgba(0, 0, 0, 0.3);
}

html.dark .login-dialog__link {
  color: #a8cdf0;
}

html.dark .login-dialog__link:hover {
  color: #bcd9f2;
}

html.dark .login-dialog__submit {
  background: linear-gradient(135deg, #4f86c6 0%, #67b7cf 100%);
  box-shadow: 0 12px 26px rgba(0, 0, 0, 0.42);
}

html.dark .login-dialog__submit:hover {
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.5);
}

/* ----- 小屏适配：隐藏侧板 ----- */
@media (max-width: 720px) {
  .login-dialog__card {
    min-height: 0;
  }

  .login-dialog__side {
    display: none;
  }

  .login-dialog__main {
    padding: 36px 26px 30px;
  }
}

/* ========== 顶栏悬浮岛（覆盖 layout.css 的通栏样式） ========== */
.app-shell > .app-shell-top {
  margin: 10px 14px 0;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.58);
  background: rgba(255, 255, 255, 0.62);
  backdrop-filter: blur(16px) saturate(1.4);
  -webkit-backdrop-filter: blur(16px) saturate(1.4);
  box-shadow: 0 10px 28px rgba(88, 111, 214, 0.14), 0 2px 8px rgba(88, 111, 214, 0.08);
}

/* 路由过渡遮罩跟随圆角，避免直角溢出 */
.app-shell > .app-shell-top::before {
  border-radius: inherit;
}

html.dark .app-shell > .app-shell-top {
  border-color: rgba(255, 255, 255, 0.12);
  background: rgba(24, 30, 52, 0.66);
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.4), 0 2px 8px rgba(0, 0, 0, 0.26);
}

/* ========== 底部音乐条悬浮岛（覆盖 layout.css 的通栏样式） ========== */
.app-shell-body__content-col > .music-bottom-bar-shell {
  width: min(860px, calc(100% - 32px));
  margin: 0 auto 14px;
  border-radius: 22px;
  border: 1px solid rgba(255, 255, 255, 0.58);
  background: rgba(255, 255, 255, 0.66);
  backdrop-filter: blur(16px) saturate(1.4);
  -webkit-backdrop-filter: blur(16px) saturate(1.4);
  box-shadow: 0 12px 32px rgba(88, 111, 214, 0.16), 0 3px 10px rgba(88, 111, 214, 0.1);
  transition: max-height 0.28s ease, opacity 0.22s ease, transform 0.22s ease,
    margin-bottom 0.28s ease;
}

/* 收起时下边距一并收掉，不留空隙 */
.app-shell-body__content-col > .music-bottom-bar-shell.is-hidden {
  margin-bottom: 0;
}

/* 内层通栏底色/边框交给外壳，避免岛内再叠一层 */
.app-shell-body__content-col > .music-bottom-bar-shell .music-bottom-bar {
  border: none;
  background: transparent;
}

html.dark .app-shell-body__content-col > .music-bottom-bar-shell {
  border-color: rgba(255, 255, 255, 0.12);
  background: rgba(24, 30, 52, 0.72);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.45), 0 3px 10px rgba(0, 0, 0, 0.3);
}

/* ========== 移动端适配（追加段，只在小屏生效，不影响桌面端） ========== */

/* 问题1（基础修复，所有宽度生效且对桌面无副作用）：
   品牌文字禁止折行、禁止被 flex 压缩，杜绝“初曦的窝”逐字竖排 */
.shell-brand > span {
  white-space: nowrap;
  flex: none;
  /* 品牌文字可点击返回首页 */
  cursor: pointer;
  transition: color 0.2s ease;
}

.shell-brand > span:hover {
  color: var(--accent-text);
}

@media (max-width: 768px) {
  /* 问题1+6：顶栏岛屿 margin 收紧为 8px 10px 0，内边距同步收紧；
     顶栏单行布局 = [品牌 | 弹性空隙 | menu 按钮 | 动作按钮组] */
  .app-shell > .app-shell-top {
    margin: 8px 10px 0;
    padding: 8px 12px;
    gap: 10px;
  }

  .app-shell-top .shell-brand {
    flex: 1 1 auto;
    min-width: 0;
    gap: 8px;
  }

  .app-shell-top .shell-brand > span {
    font-size: 16.5px;
  }

  /* menu 按钮（在 .shell-brand 内的 popover 包装器里）推到品牌区右端，
     形成品牌与 menu 之间的弹性空隙 */
  .app-shell-top .shell-brand > .lx-popover-wrapper {
    margin-left: auto;
    flex: none;
  }

  /* 触控目标 ≥40px（含 menu 按钮，均为 .shell-action-btn） */
  .app-shell-top .shell-action-btn {
    width: 40px;
    height: 40px;
  }

  .app-shell-top .shell-actions {
    gap: 5px;
  }

  /* 问题4（AI 侧栏）：layout.css 在 ≤768px 隐藏了 is-ai 按钮，
     但它是移动端 AI 侧栏唯一入口（live2d 入口也已隐藏），恢复显示；
     侧栏本体的 absolute 覆盖层规则 layout.css 原生已有，无需 JS 改动 */
  .shell-actions .shell-action-btn.is-ai {
    display: inline-flex;
  }

  /* AI 侧栏展开时强制满宽兜底（覆盖层位于 .app-shell-body 内，
     不受顶栏悬浮岛 margin 影响） */
  .layout-right-sidebar.is-expanded {
    width: 100%;
    flex-basis: 100%;
    max-width: 100%;
  }

  /* 问题2：live2d 看板娘（含四个浮动操作按钮）在移动端整体隐藏 */
  .live2d-widget {
    display: none !important;
  }

  /* 问题3（音乐岛）：宽度改为 calc(100% - 20px)；
     track-left 的文字兜底隐藏（layout.css ≤860px 已隐藏整个 .track-left） */
  .app-shell-body__content-col > .music-bottom-bar-shell {
    width: calc(100% - 20px);
  }

  .music-bottom-bar .track-left .meta-wrap {
    display: none;
  }

  /* 问题5（搜索浮层）：面板宽度 calc(100vw - 24px)，内边距收紧
     （加 .app-shell 前缀提升优先级，压过 layout.css 的 ≤900/≤640 同名规则） */
  .app-shell .layout-article-search-overlay {
    padding: 8vh 12px 20px;
  }

  .app-shell .layout-article-search-panel {
    width: calc(100vw - 24px);
    padding: 12px;
    gap: 12px;
  }

  /* 问题5（设置弹窗）：宽度 calc(100vw - 24px)、内边距收紧、图库改 3 列网格 */
  .app-shell .setting-dialog {
    padding: 12px;
  }

  .app-shell .setting-dialog__card {
    width: calc(100vw - 24px);
    padding: 20px 14px 16px;
  }

  .setting-dialog .gallery-strip {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 10px;
    overflow-x: visible;
    padding-bottom: 4px;
  }

  .setting-dialog .gallery-item,
  .setting-dialog .gallery-item.is-portrait {
    width: 100%;
    height: 64px;
  }

  .setting-dialog .gallery-item img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }

  .setting-dialog .gallery-item__label {
    left: 8px;
    bottom: 8px;
    padding: 3px 8px;
    font-size: 11px;
  }

  /* 问题5（登录弹窗）：≤720px 侧板已隐藏，这里收紧外层留白；
     person 菜单 / 移动导航菜单宽度不超出视口 */
  .app-shell .login-dialog {
    padding: 12px;
  }

  .lx-popover.login-person-popover,
  .lx-popover.top-nav-mobile-popover {
    max-width: calc(100vw - 24px);
  }
}

@media (max-width: 480px) {
  /* 问题1（≤480 细化）：品牌 14px、按钮间距进一步收紧，
     并隐藏两个次要按钮（猫爪 is-cat 及其吊绳容器、设置 is-setting），
     确保 375px 宽度下 [品牌|空隙|menu|search/theme/ai/music/person] 单行放下 */
  .app-shell > .app-shell-top {
    padding: 8px 10px;
  }

  .app-shell-top .shell-brand > span {
    font-size: 15.5px;
  }

  .app-shell-top .shell-actions {
    gap: 3px;
  }

  .app-shell-top .shell-action-cat-wrap,
  .app-shell-top .shell-action-btn.is-setting {
    display: none;
  }

  /* 问题3（≤480 细化）：隐藏快退/快进两个次要按钮，
     避免控制行与右上角 播放列表/关闭 按钮（absolute）重叠 */
  .music-bottom-bar .control-btn.is-seek-back,
  .music-bottom-bar .control-btn.is-seek-forward,
  .music-bottom-bar .control-btn.is-repeat-dup {
    display: none;
  }

  /* 问题5（登录弹窗 ≤480 细化）：表单区内边距与标题字号收紧 */
  .login-dialog__main {
    padding: 28px 18px 24px;
  }

  .login-dialog__title {
    font-size: 24px;
  }
}

/* Tab 下划线：常态停在当前页，悬停跟随，移开带弹性回弹 */
.app-shell .shell-nav .nav-underline {
  position: absolute;
  left: 0;
  bottom: 2px;
  height: 3px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(109, 155, 214, 0.35), #3f77b5 45%, rgba(109, 155, 214, 0.35));
  box-shadow: 0 2px 8px rgba(63, 119, 181, 0.4);
  opacity: 0;
  pointer-events: none;
  z-index: 2;
  transition:
    transform 0.42s cubic-bezier(0.34, 1.56, 0.64, 1),
    width 0.32s cubic-bezier(0.34, 1.56, 0.64, 1),
    opacity 0.2s ease;
}
html.dark .app-shell .shell-nav .nav-underline {
  background: linear-gradient(90deg, rgba(140, 190, 240, 0.3), #8cbef0 45%, rgba(140, 190, 240, 0.3));
  box-shadow: 0 2px 10px rgba(140, 190, 240, 0.35);
}
@media (max-width: 768px) {
  .app-shell .shell-nav .nav-underline { display: none; }
}
</style>
