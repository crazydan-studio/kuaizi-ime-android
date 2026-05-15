#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
DOCS_DIR="$PROJECT_DIR/docs"
PORT="${1:-5173}"

echo "=== 筷字输入法 — 文档预览服务 ==="
echo "  目录: $DOCS_DIR"
echo "  端口: $PORT"
echo "  包管理器: pnpm"
echo "  URL: http://localhost:$PORT"
echo ""
echo "按 Ctrl+C 停止服务。"
echo ""

cd "$DOCS_DIR"
mkdir -p public/diagrams
pnpm install || exit $?

pnpm dev --port "$PORT"
