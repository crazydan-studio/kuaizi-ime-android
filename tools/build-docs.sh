#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
DOCS_DIR="$PROJECT_DIR/docs"
OUTPUT_DIR="$DOCS_DIR/.vitepress/dist"

ACTION="${1:-build}"

usage() {
  echo "用法: $(basename "$0") [build|preview|deploy]"
  echo ""
  echo "  build   — 静态构建文档到 $OUTPUT_DIR"
  echo "  preview — 构建后本地预览"
  echo "  deploy  — 构建并部署到 GitHub Pages"
  exit 1
}

cd "$DOCS_DIR"
pnpm install || exit $?

case "$ACTION" in
  build)
    echo "=== 构建文档 ==="
    pnpm build
    echo ""
    echo "构建完成: $OUTPUT_DIR"
    ;;
  preview)
    echo "=== 构建并预览文档 ==="
    pnpm build
    echo ""
    echo "构建完成，启动预览..."
    pnpm preview
    ;;
  deploy)
    echo "=== 构建并部署文档 ==="
    pnpm build

    # 将 .nojekyll 放入输出目录以兼容 GitHub Pages
    touch "$OUTPUT_DIR/.nojekyll"

    echo ""
    echo "构建完成: $OUTPUT_DIR"
    echo ""
    echo "部署方式："
    echo "  1. 将 $OUTPUT_DIR 内容推送到 gh-pages 分支"
    echo "  2. 或通过 GitHub Actions 自动部署"
    ;;
  *)
    usage
    ;;
esac
