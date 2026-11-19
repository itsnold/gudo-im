@echo off
setlocal
call "%~dp0Run-Gudo.bat" --setup
exit /b %errorlevel%
