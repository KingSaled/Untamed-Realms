@echo off
title Untamed Realms - install modpack
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\install-client.ps1"
if errorlevel 1 echo. & echo Something went wrong - see the message above. Run this file again to retry.
echo.
pause
