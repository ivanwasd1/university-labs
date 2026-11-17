@echo off
chcp 65001 >nul
cd /d "%~dp0"
if not exist out mkdir out
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -d out @sources.txt
if errorlevel 1 goto compilefailed
java -Dfile.encoding=UTF-8 -cp out Main %*
if errorlevel 1 goto failed
pause
exit /b 0
:failed
echo Запуск завершился ошибкой. Проверьте сообщение выше.
pause
exit /b 1
:compilefailed
echo Не удалось собрать проект. Требуется JDK 17 или новее.
pause
exit /b 1
