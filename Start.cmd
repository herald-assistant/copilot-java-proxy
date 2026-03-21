@echo off
setlocal EnableExtensions
cd /d "%~dp0"
set "JAR=herald-copilot-connector-0.1.0.jar"
start "" javaw -jar "%~dp0%JAR%"
endlocal