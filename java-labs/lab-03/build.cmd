@echo off
cd /d "%~dp0"
if not exist build mkdir build
powershell -NoProfile -Command "Get-ChildItem build/*.class,build/*.jar -ErrorAction SilentlyContinue | Remove-Item -Force"
if errorlevel 1 exit /b 1
javac -encoding UTF-8 -source 11 -target 11 -Xlint:-options -cp "lib/*" -d build src/*.java
if errorlevel 1 exit /b 1
jar cfe "%~dp0build\quadrilaterals.jar" Main -C "%~dp0build" .
if errorlevel 1 exit /b 1
exit /b 0
