@echo off
call "%~dp0build.bat"
if errorlevel 1 exit /b %errorlevel%
java -jar "%~dp0target\ColorModelsLab.jar"
exit /b %errorlevel%
