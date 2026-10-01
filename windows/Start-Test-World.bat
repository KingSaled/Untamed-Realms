@echo off
title Untamed Realms - TEST WORLD server
rem Same server and mods as Start-Server.bat, but the separate "test-world" save with the test hub.
set UR_TEST_WORLD=1
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-server.ps1"
if errorlevel 1 echo. & echo Something went wrong - see the message above. Run this file again to retry.
echo.
pause
