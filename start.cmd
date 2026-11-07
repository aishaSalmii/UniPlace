@echo off
rem Starts the UniPlace Engine (127.0.0.1:8081) and the UniPlace Portal (127.0.0.1:8080).
rem   start.cmd          start with the existing data, Portal built from source with the Maven wrapper
rem   start.cmd --jar    start the prebuilt Portal (bin\uniplace-portal.jar): nothing is downloaded
rem   start.cmd --reset  delete the Portal data and load the demonstration data again (combine with --jar if needed)
setlocal
cd /d "%~dp0"
set "ROOT=%CD%"

set USE_JAR=0
for %%a in (%*) do (
  if /i "%%~a"=="--reset" (
    if exist portal\data rmdir /s /q portal\data
    if exist portal\data (
      echo Could not delete portal\data: stop the running Portal first.
      exit /b 1
    )
    echo Portal data deleted; the demonstration data will be loaded again.
  ) else if /i "%%~a"=="--jar" (
    set USE_JAR=1
  ) else (
    echo Unknown option: %%~a
    exit /b 2
  )
)

where java >nul 2>nul
if errorlevel 1 (
  echo Java 17 or later is required ^(java was not found on the PATH^).
  exit /b 1
)

rem The Maven wrapper needs JAVA_HOME: derive it from the java on the PATH when it is not set
if not defined JAVA_HOME (
  for /f "tokens=2 delims==" %%h in ('java -XshowSettings:properties -version 2^>^&1 ^| findstr /c:"java.home"') do (
    for /f "tokens=*" %%t in ("%%h") do set "JAVA_HOME=%%t"
  )
)

if not exist logs mkdir logs
call :engine_up
if not errorlevel 1 (
  echo An Engine is already answering on 127.0.0.1:8081; using it.
  goto portal
)
start "UniPlace Engine - close this window to stop it" /min cmd /c "java -jar engine\uniplace-engine.jar > logs\engine.log 2>&1"
call :wait_engine 30
if errorlevel 1 (
  echo The Engine did not start; see logs\engine.log.
  exit /b 1
)
echo Engine running on http://127.0.0.1:8081/api/v1 ^(log: logs\engine.log^)

:portal
cd portal
if "%USE_JAR%"=="1" (
  if not exist "%ROOT%\bin\uniplace-portal.jar" (
    echo bin\uniplace-portal.jar not found: it is not kept in Git; copy it from the UniPlace package, or start without --jar.
    exit /b 1
  )
  echo Starting the prebuilt Portal on http://127.0.0.1:8080 ...
  java -jar "%ROOT%\bin\uniplace-portal.jar"
) else (
  echo Starting the Portal on http://127.0.0.1:8080 ... the first start downloads dependencies and takes a few minutes.
  call mvnw.cmd -q spring-boot:run
)
endlocal
exit /b 0

:engine_up
where curl >nul 2>nul
if errorlevel 1 goto engine_log
curl -s -f -o nul http://127.0.0.1:8081/api/v1/health >nul 2>nul
exit /b %errorlevel%
:engine_log
findstr /c:"listening" logs\engine.log >nul 2>nul && exit /b 0
exit /b 1

:wait_engine
for /l %%i in (1,1,%1) do (
  call :engine_up
  if not errorlevel 1 exit /b 0
  ping -n 2 127.0.0.1 >nul
)
exit /b 1
