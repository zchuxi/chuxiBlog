@echo off
chcp 65001 >nul
cd /d "%~dp0frontend"
if not exist node_modules (
  echo [install] node_modules not found, installing...
  call npm install --no-audit --no-fund
)
echo [run] starting vite dev server on http://localhost:5173 ...
npm run dev
pause
