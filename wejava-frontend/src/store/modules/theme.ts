import { defineStore } from 'pinia'

export type ThemeMode = 'light' | 'dark' | 'system'
export type ThemePreset = 'default' | 'vibrant' | 'calm'
export type MotionLevel = 'full' | 'fast' | 'none'

interface PresetConfig {
  key: ThemePreset
  label: string
  primary: string
}

/** 主题色预设：欢快=暖橙，轻松=青绿 */
export const PRESETS: PresetConfig[] = [
  { key: 'default', label: '默认蓝', primary: '#409eff' },
  { key: 'vibrant', label: '欢快橙', primary: '#ff7d00' },
  { key: 'calm', label: '轻松青', primary: '#14b8a6' }
]

const MODE_KEY = 'wejava:theme-mode'
const PRESET_KEY = 'wejava:theme-preset'
const MOTION_KEY = 'wejava:motion'
const DARK_BG: [number, number, number] = [20, 20, 20]
const LIGHT_BG: [number, number, number] = [255, 255, 255]

function hexToRgb(hex: string): [number, number, number] {
  const value = hex.replace('#', '')
  return [
    parseInt(value.slice(0, 2), 16),
    parseInt(value.slice(2, 4), 16),
    parseInt(value.slice(4, 6), 16)
  ]
}

function mix(hex: string, target: [number, number, number], weight: number): string {
  const [r, g, b] = hexToRgb(hex)
  const to = (from: number, dest: number) => Math.round(from * (1 - weight) + dest * weight)
  return `rgb(${to(r, target[0])}, ${to(g, target[1])}, ${to(b, target[2])})`
}

export const useThemeStore = defineStore('theme', () => {
  const mode = ref<ThemeMode>((localStorage.getItem(MODE_KEY) as ThemeMode) || 'light')
  const preset = ref<ThemePreset>((localStorage.getItem(PRESET_KEY) as ThemePreset) || 'default')
  const motion = ref<MotionLevel>((localStorage.getItem(MOTION_KEY) as MotionLevel) || 'full')

  const media = window.matchMedia('(prefers-color-scheme: dark)')
  const systemDark = ref(media.matches)

  const isDark = computed(() => (mode.value === 'system' ? systemDark.value : mode.value === 'dark'))
  const primary = computed(() => PRESETS.find((p) => p.key === preset.value)?.primary ?? PRESETS[0].primary)

  function apply() {
    const root = document.documentElement
    root.classList.toggle('dark', isDark.value)
    root.dataset.preset = preset.value
    root.dataset.motion = motion.value

    const base = primary.value
    // 明暗模式决定浅色梯度朝背景色混合，而不是永远朝白色
    const blendTarget = isDark.value ? DARK_BG : LIGHT_BG
    root.style.setProperty('--el-color-primary', base)
    for (const level of [3, 5, 7, 8, 9]) {
      root.style.setProperty(`--el-color-primary-light-${level}`, mix(base, blendTarget, level / 10))
    }
    root.style.setProperty('--el-color-primary-dark-2', mix(base, [0, 0, 0], 0.2))
  }

  function setMode(value: ThemeMode) {
    mode.value = value
    localStorage.setItem(MODE_KEY, value)
    apply()
  }

  function setPreset(value: ThemePreset) {
    preset.value = value
    localStorage.setItem(PRESET_KEY, value)
    apply()
  }

  function setMotion(value: MotionLevel) {
    motion.value = value
    localStorage.setItem(MOTION_KEY, value)
    apply()
  }

  function init() {
    apply()
    media.addEventListener('change', (event) => {
      systemDark.value = event.matches
      if (mode.value === 'system') apply()
    })
  }

  return { mode, preset, motion, isDark, primary, setMode, setPreset, setMotion, init }
})
