#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
DOCS_DIR="$PROJECT_DIR/docs"
PORT="${1:-3000}"

# Check for pnpm or npm
if command -v pnpm &>/dev/null; then
  NPM_CMD="pnpm"
elif command -v npm &>/dev/null; then
  NPM_CMD="npm"
else
  echo "Error: Neither pnpm nor npm found. Please install Node.js first."
  exit 1
fi

echo "Starting documentation preview server..."
echo "  Directory: $DOCS_DIR"
echo "  Port: $PORT"
echo "  Package manager: $NPM_CMD"
echo "  URL: http://localhost:$PORT"
echo ""
echo "Press Ctrl+C to stop the server."
echo ""

npx docsify-cli serve "$DOCS_DIR" --port "$PORT"
