@echo off
chcp 65001 >nul
cd /d "%~dp0backend"
set DB_PASSWORD=1234
set SERVER_PORT=8081
if not exist target\blog-backend.jar (
  echo [build] target\blog-backend.jar not found, packaging...
  call mvn -DskipTests package
)
echo [run] starting backend on port %SERVER_PORT% ...
java -Dfile.encoding=UTF-8 -jar target\blog-backend.jar
pause
