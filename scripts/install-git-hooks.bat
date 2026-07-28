@echo off
rem 安装仓库版本化的 git hooks 到 .git\hooks\（当前仅 pre-commit：backend/ 改动提交前强制 mvn test，frontend/ 改动提交前强制 npm run lint）
setlocal
cd /d "%~dp0.."
if not exist ".git\hooks" (
  echo [install-git-hooks] 未找到 .git\hooks 目录，请在仓库根目录下运行
  exit /b 1
)
copy /y "scripts\git-hooks\pre-commit" ".git\hooks\pre-commit" >nul
if errorlevel 1 (
  echo [install-git-hooks] 复制失败
  exit /b 1
)
echo [install-git-hooks] 已安装 pre-commit 钩子：backend/ 改动提交时自动运行 mvn test，frontend/ 改动提交时自动运行 npm run lint
endlocal
