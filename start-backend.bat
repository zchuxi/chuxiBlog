@echo off
chcp 65001 >nul
cd /d "%~dp0backend"
set DB_PASSWORD=1234
set SERVER_PORT=8081
rem ===== 阿里云 OSS（媒体存储）=====
rem AK/SK 存于 Windows 用户级环境变量 APP_OSS_ACCESS_KEY_ID / APP_OSS_ACCESS_KEY_SECRET，
rem 进程启动时自动继承，不再从工作区明文脚本加载；未配置则上传回退本地 uploads/ 目录
rem 默认 bucket=chuxisleep，北京地域；如有变动可覆盖：
rem set APP_OSS_BUCKET=chuxisleep
rem set APP_OSS_ENDPOINT=https://oss-cn-beijing.aliyuncs.com
if not exist target\chuxi-backend.jar (
  echo [build] target\chuxi-backend.jar not found, packaging...
  call mvn -DskipTests package
)
echo [run] starting backend on port %SERVER_PORT% ...
java -Dfile.encoding=UTF-8 -jar target\chuxi-backend.jar
pause
