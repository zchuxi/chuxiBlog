# 用法: pwsh scripts/make-favicon.ps1 [输入图路径]
# 功能: 裁掉四周白边 -> 圆形透明遮罩 -> 输出 frontend/public/favicon.png (256x256)
param([string]$In = "$PSScriptRoot\..\avatar.png")

Add-Type -AssemblyName System.Drawing
$src = [System.Drawing.Bitmap]::FromFile((Resolve-Path $In))

# 1. 扫描非白色像素包围盒（阈值 245）
$minX = $src.Width; $minY = $src.Height; $maxX = 0; $maxY = 0
$step = [Math]::Max(1, [int]($src.Width / 600))
for ($y = 0; $y -lt $src.Height; $y += $step) {
  for ($x = 0; $x -lt $src.Width; $x += $step) {
    $p = $src.GetPixel($x, $y)
    if ($p.A -gt 40 -and -not ($p.R -gt 245 -and $p.G -gt 245 -and $p.B -gt 245)) {
      if ($x -lt $minX) { $minX = $x }; if ($x -gt $maxX) { $maxX = $x }
      if ($y -lt $minY) { $minY = $y }; if ($y -gt $maxY) { $maxY = $y }
    }
  }
}
$w = $maxX - $minX + 1; $h = $maxY - $minY + 1
$side = [Math]::Min($w, $h)
$cx = $minX + [int]($w / 2); $cy = $minY + [int]($h / 2)
$half = [int]($side / 2)
$crop = New-Object System.Drawing.Rectangle(($cx - $half), ($cy - $half), $side, $side)

# 2. 裁方 + 缩放 256 + 圆形遮罩
$out = New-Object System.Drawing.Bitmap 256, 256
$g = [System.Drawing.Graphics]::FromImage($out)
$g.SmoothingMode = 'AntiAlias'
$g.InterpolationMode = 'HighQualityBicubic'
$path = New-Object System.Drawing.Drawing2D.GraphicsPath
$path.AddEllipse(0, 0, 256, 256)
$g.SetClip($path)
$g.DrawImage($src, (New-Object System.Drawing.Rectangle(0, 0, 256, 256)), $crop, [System.Drawing.GraphicsUnit]::Pixel)
$g.Dispose()

$dest = Join-Path $PSScriptRoot "..\frontend\public\favicon.png"
$out.Save($dest, [System.Drawing.Imaging.ImageFormat]::Png)
$src.Dispose(); $out.Dispose()
Write-Host "favicon 已生成: $dest (裁切区域 $($crop.X),$($crop.Y) ${side}x${side})"
