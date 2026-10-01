@echo off
title Untamed Realms - fresh world
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\reset-world.ps1"
if errorlevel 1 echo. & echo Something went wrong - see the message above.
echo.
pause
