@echo off
title Untamed Realms - fresh TEST world
set UR_TEST_WORLD=1
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\reset-world.ps1"
if errorlevel 1 echo. & echo Something went wrong - see the message above.
echo.
pause
