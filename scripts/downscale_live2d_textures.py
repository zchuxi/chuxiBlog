#!/usr/bin/env python3
"""Live2D 贴图降采样：miku.4096 -> miku.2048（或指定档位）。

背景：miku 模型自带 6 张 4096x4096 PNG（共 25.4MB），而看板娘在页面上
最大只显示 270x430 CSS 像素（DPR 3 下约 810x1290）。4096 贴图带来三重开销：
  - 下载 25.4MB
  - 单张主线程解码约 208ms，6 张约 1.2s
  - 解码后 GPU 显存单张 64MB，6 张 384MB（含 mipmap 约 510MB）

本脚本只生成新目录，不修改也不删除原始 miku.4096/，以符合模型
「不可二改」的授权约束——原图完整保留，运行时改为加载降采样副本。

用法：
    python scripts/downscale_live2d_textures.py            # 生成 2048
    python scripts/downscale_live2d_textures.py --size 1024
    python scripts/downscale_live2d_textures.py --force    # 覆盖已存在的输出
    python scripts/downscale_live2d_textures.py --dry-run  # 只打印计划
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

try:
    from PIL import Image
except ImportError:  # pragma: no cover - 环境缺依赖时给出可执行的提示
    sys.exit('需要 Pillow：python -m pip install Pillow')

REPO_ROOT = Path(__file__).resolve().parent.parent
MIKU_DIR = REPO_ROOT / 'frontend' / 'public' / 'live2d' / 'miku'
SOURCE_DIR = MIKU_DIR / 'miku.4096'


def human_mb(size: int) -> str:
    return f'{size / 1024 / 1024:.2f}MB'


def downscale(source: Path, target: Path, size: int, dry_run: bool) -> tuple[int, int]:
    """把单张贴图缩到 size x size，返回 (原体积, 新体积)。"""
    original = source.stat().st_size
    if dry_run:
        return original, 0

    with Image.open(source) as image:
        # 保留 alpha：Live2D 贴图依赖透明通道做遮罩
        rgba = image.convert('RGBA')
        # LANCZOS 对这类线稿/色块混合的立绘贴图边缘保持最好
        resized = rgba.resize((size, size), Image.Resampling.LANCZOS)
        target.parent.mkdir(parents=True, exist_ok=True)
        # optimize 让 PNG 编码器多尝试几种过滤器，体积再降一点
        resized.save(target, format='PNG', optimize=True)

    return original, target.stat().st_size


def main() -> int:
    parser = argparse.ArgumentParser(description='Live2D 贴图降采样')
    parser.add_argument('--size', type=int, default=2048, help='输出边长（默认 2048）')
    parser.add_argument('--force', action='store_true', help='覆盖已存在的输出文件')
    parser.add_argument('--dry-run', action='store_true', help='只打印计划，不写文件')
    args = parser.parse_args()

    if not SOURCE_DIR.is_dir():
        print(f'找不到源贴图目录: {SOURCE_DIR}', file=sys.stderr)
        return 1

    sources = sorted(SOURCE_DIR.glob('texture_*.png'))
    if not sources:
        print(f'源目录没有 texture_*.png: {SOURCE_DIR}', file=sys.stderr)
        return 1

    out_dir = MIKU_DIR / f'miku.{args.size}'
    print(f'源: {SOURCE_DIR}')
    print(f'出: {out_dir}  ({args.size}x{args.size})')
    print()

    total_before = 0
    total_after = 0
    written = 0

    for source in sources:
        target = out_dir / source.name
        if target.exists() and not args.force and not args.dry_run:
            size_after = target.stat().st_size
            total_before += source.stat().st_size
            total_after += size_after
            print(f'[skip] {source.name}: 已存在 {human_mb(size_after)}（--force 可覆盖）')
            continue

        before, after = downscale(source, target, args.size, args.dry_run)
        total_before += before
        total_after += after
        if args.dry_run:
            print(f'[plan] {source.name}: {human_mb(before)} -> {args.size}x{args.size}')
        else:
            written += 1
            pct = (1 - after / before) * 100 if before else 0
            print(f'[ok]   {source.name}: {human_mb(before)} -> {human_mb(after)}  (-{pct:.0f}%)')

    print()
    if args.dry_run:
        print(f'共 {len(sources)} 张，源合计 {human_mb(total_before)}（未写入任何文件）')
        return 0

    pct = (1 - total_after / total_before) * 100 if total_before else 0
    print(f'共 {len(sources)} 张（本次写入 {written} 张）')
    print(f'合计: {human_mb(total_before)} -> {human_mb(total_after)}  (-{pct:.0f}%)')

    # 显存是比体积更关键的收益：4 字节/像素，与 PNG 压缩率无关
    vram_before = len(sources) * 4096 * 4096 * 4 / 1024 / 1024
    vram_after = len(sources) * args.size * args.size * 4 / 1024 / 1024
    print(f'解码后显存: {vram_before:.0f}MB -> {vram_after:.0f}MB')
    return 0


if __name__ == '__main__':
    sys.exit(main())
