import sprite from '../assets/svg-sprite.svg?raw'

export function injectSvgSprite() {
  if (document.getElementById('__svg__icons__dom__')) return
  const holder = document.createElement('div')
  holder.innerHTML = sprite
  const svg = holder.firstElementChild
  if (svg) {
    svg.setAttribute('aria-hidden', 'true')
    document.body.appendChild(svg)
  }
}
