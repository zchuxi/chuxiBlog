// v-reveal：进入视口时 viewport-reveal-pending -> viewport-reveal-visible
const observer = typeof IntersectionObserver !== 'undefined'
  ? new IntersectionObserver(entries => {
      for (const entry of entries) {
        if (entry.isIntersecting) {
          entry.target.classList.remove('viewport-reveal-pending')
          entry.target.classList.add('viewport-reveal-visible')
          observer.unobserve(entry.target)
        }
      }
    }, { threshold: 0.12 })
  : null

export default {
  mounted(el, binding) {
    el.classList.add('viewport-reveal', 'viewport-reveal-pending')
    if (binding.value != null) {
      el.style.setProperty('--viewport-reveal-delay', `${binding.value}ms`)
    }
    if (observer) observer.observe(el)
    else el.classList.replace('viewport-reveal-pending', 'viewport-reveal-visible')
  },
  unmounted(el) {
    if (observer) observer.unobserve(el)
  }
}
