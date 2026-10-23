@echo off
rem Compila o backend Java e abre o saloon em http://localhost:8080
setlocal
cd /d "%~dp0"
call "%~dp0scripts\achar-jdk.bat" || exit /b 1

echo Compilando o backend...
if exist build\classes rmdir /s /q build\classes
mkdir build\classes 2>nul
dir /s /b backend\src\main\java\*.java > build\fontes.txt
"%JDK_BIN%\javac.exe" --release 17 -encoding UTF-8 -d build\classes @build\fontes.txt || (echo Falha na compilacao. & exit /b 1)

set PORTA=%1
if "%PORTA%"=="" set PORTA=8080
start "" http://localhost:%PORTA%
"%JDK_BIN%\java.exe" -Dfile.encoding=UTF-8 -cp build\classes br.com.truco.web.Servidor %PORTA% frontend
