@echo off
title Untamed Realms - server
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-server.ps1"
if errorlevel 1 echo. & echo Something went wrong - see the message above. Run this file again to retry.
echo.
pause
