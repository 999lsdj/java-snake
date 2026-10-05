@echo off
rem Portable launcher for Windows: double-click this file to start the game.
rem It looks for the jar next to this script or in the dist\ folder.
setlocal
set "HERE=%~dp0"
set "JAR="

for %%F in ("%HERE%dist\java-snake-*.jar") do if exist "%%~fF" set "JAR=%%~fF"
if not defined JAR for %%F in ("%HERE%java-snake-*.jar") do if exist "%%~fF" set "JAR=%%~fF"

if not defined JAR (
  echo [java-snake] Cannot find the jar.
  echo [java-snake] Build it with "mvn package" then copy it into dist\, or download it from GitHub Releases.
  pause
  exit /b 1
)

set "JAVAW=javaw.exe"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javaw.exe" set "JAVAW=%JAVA_HOME%\bin\javaw.exe"

start "" "%JAVAW%" -jar "%JAR%"
exit /b 0
