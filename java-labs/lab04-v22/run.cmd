@echo off
chcp 65001 >nul
cd /d "%~dp0"
call build.cmd
if errorlevel 1 goto finish
java -ea -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -jar "%~dp0build\quadrilaterals.jar"
:finish
set result=%errorlevel%
pause
exit /b %result%
