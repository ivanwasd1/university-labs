@echo off
pushd "%~dp0" || exit /b 1
call mvn -B test
set "result=%errorlevel%"
popd
exit /b %result%
