@echo off
setlocal EnableExtensions
cd /d "%~dp0"

set "REQ_MAJOR=23"
set "JAR=herald-copilot-connector-0.1.0.jar"
echo Required Java version: %REQ_MAJOR%

set "CUR_MAJOR="

where java >nul 2>&1
if not errorlevel 1 (
  for /f "tokens=2 delims== " %%V in ('java -XshowSettings:properties -version 2^>^&1 ^| findstr /i "java.specification.version"') do (
    for /f "tokens=1,2 delims=." %%a in ("%%V") do (
      if "%%a"=="1" (set "CUR_MAJOR=%%b") else (set "CUR_MAJOR=%%a")
    )
  )
)
echo Current Java version: %CUR_MAJOR%

if not defined CUR_MAJOR goto INSTALL_JAVA
set /a CUR_NUM=%CUR_MAJOR% >nul 2>&1
if errorlevel 1 goto INSTALL_JAVA
if %CUR_NUM% LSS %REQ_MAJOR% goto INSTALL_JAVA
goto JAVA_OK

:INSTALL_JAVA
echo UPDATING JAVA TO %REQ_MAJOR%
winget install Microsoft.OpenJDK.%REQ_MAJOR% --scope user --accept-package-agreements --accept-source-agreements

:JAVA_OK
where copilot >nul 2>&1
if errorlevel 1 (
  echo INSTALLING Copilot
  winget install Microsoft.PowerShell --scope user --accept-package-agreements --accept-source-agreements
  winget install GitHub.Copilot --scope user --accept-package-agreements --accept-source-agreements
)

start "" javaw -jar "%~dp0%JAR%"
endlocal