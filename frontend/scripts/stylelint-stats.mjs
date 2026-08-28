// 一次性统计脚本：输出指定规则的违规明细，供存量豁免策略参考
import stylelint from 'stylelint'

const targets = process.argv.slice(2)
const result = await stylelint.lint({ files: 'src/**/*.{css,vue}' })
for (const file of result.results) {
  for (const w of file.warnings) {
    if (targets.length && !targets.includes(w.rule)) continue
    const rel = file.source.replace(/^.*[\\/]frontend[\\/]/, '')
    console.log(`${rel}:${w.line}:${w.column} [${w.rule}] ${w.text}`)
  }
}
