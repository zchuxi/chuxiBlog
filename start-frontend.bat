@echo off
chcp 65001 >nul
rem 非阻断检测：提示未安装 pre-commit 钩子（不自动安装）
if not exist "%~dp0.git\hooks\pre-commit" (
  echo [hint] 未安装 pre-commit 钩子，请运行 scripts\install-git-hooks.bat
)
cd /d "%~dp0frontend"
if not exist node_modules (
  echo [install] node_modules not found, installing...
  call npm install --no-audit --no-fund
)
echo [run] starting vite dev server on http://localhost:5173 ...
npm run dev
pause
