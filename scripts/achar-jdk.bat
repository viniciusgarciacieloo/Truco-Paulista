@echo off
rem Procura um JDK 17+ e define JDK_BIN (pasta com javac.exe e java.exe).
rem Ordem: JAVA_HOME, javac no PATH, instalacoes do Temurin.
set "JDK_BIN="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" set "JDK_BIN=%JAVA_HOME%\bin"
if not defined JDK_BIN for /f "delims=" %%i in ('where javac 2^>nul') do if not defined JDK_BIN set "JDK_BIN=%%~dpi"
if not defined JDK_BIN for /d %%d in ("%ProgramFiles%\Eclipse Adoptium\jdk-*") do set "JDK_BIN=%%d\bin"
if defined JDK_BIN if "%JDK_BIN:~-1%"=="\" set "JDK_BIN=%JDK_BIN:~0,-1%"
if not defined JDK_BIN (
  echo Nao encontrei um JDK. Instale com:  winget install EclipseAdoptium.Temurin.21.JDK
  exit /b 1
)
exit /b 0
