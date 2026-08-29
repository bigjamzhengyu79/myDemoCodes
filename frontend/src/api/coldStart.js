/**
 * 冷启动提示。
 *
 * 后端部署在 Render 免费实例上，闲置后会休眠，首次请求需要重新拉起整个
 * Spring Boot 应用 —— 实测 90 秒以上，预热后普通请求在 1 秒内返回。
 *
 * 这段时间里页面没有任何反馈，用户会以为卡死（历史上这里是 10 秒就直接超时报错）。
 * 所以：请求超过 SLOW_THRESHOLD 仍未返回，就在界面顶部显示「服务启动中」。
 *
 * 用计数器而不是布尔值，因为页面加载时会有多个并发请求，
 * 任意一个先返回都不该把提示关掉。
 */
import { ref, computed } from 'vue'

const SLOW_THRESHOLD = 3000

// 当前「已经慢到需要提示」的请求数
const slowRequestCount = ref(0)

export const isColdStarting = computed(() => slowRequestCount.value > 0)

/**
 * 标记一个请求开始。返回的函数必须在请求结束时调用（成功或失败都要）。
 */
export function trackRequest() {
  let countedAsSlow = false

  const timer = setTimeout(() => {
    countedAsSlow = true
    slowRequestCount.value++
  }, SLOW_THRESHOLD)

  let settled = false
  return function done() {
    // 防止重复调用把计数减成负数
    if (settled) return
    settled = true

    clearTimeout(timer)
    if (countedAsSlow) {
      slowRequestCount.value--
    }
  }
}

/**
 * 给 axios 实例装上冷启动跟踪。
 *
 * 注意：计时器句柄挂在 config 上传递，因为请求拦截器和响应拦截器之间
 * 没有其它共享上下文；响应拦截器要从 err.config 上取回它。
 */
export function attachColdStartTracking(instance) {
  instance.interceptors.request.use(config => {
    config.__coldStartDone = trackRequest()
    return config
  })

  instance.interceptors.response.use(
    res => {
      res.config?.__coldStartDone?.()
      return res
    },
    err => {
      err.config?.__coldStartDone?.()
      return Promise.reject(err)
    }
  )

  return instance
}
