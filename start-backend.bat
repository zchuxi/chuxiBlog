@echo off
chcp 65001 >nul
cd /d "%~dp0backend"
set DB_PASSWORD=1234
set SERVER_PORT=8081
rem ===== 阿里云 OSS（媒体存储）=====
rem 密钥放在仓库根目录 oss-keys.bat（已被 .gitignore 排除），存在则自动加载
if exist "%~dp0oss-keys.bat" call "%~dp0oss-keys.bat"
rem 默认 bucket=chuxisleep，北京地域；如有变动可覆盖：
rem set OSS_BUCKET=chuxisleep
rem set OSS_ENDPOINT=https://oss-cn-beijing.aliyuncs.com
if not exist target\blog-backend.jar (
  echo [build] target\blog-backend.jar not found, packaging...
  call mvn -DskipTests package
)
echo [run] starting backend on port %SERVER_PORT% ...
java -Dfile.encoding=UTF-8 -jar target\blog-backend.jar
pause
