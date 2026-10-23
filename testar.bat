@echo off
rem Compila e roda os testes das regras (cenarios T1-T13)
setlocal
cd /d "%~dp0"
call "%~dp0scripts\achar-jdk.bat" || exit /b 1

if exist build\testes rmdir /s /q build\testes
mkdir build\testes 2>nul
dir /s /b backend\src\main\java\*.java backend\src\test\java\*.java > build\fontes-testes.txt
"%JDK_BIN%\javac.exe" --release 17 -encoding UTF-8 -d build\testes @build\fontes-testes.txt || (echo Falha na compilacao. & exit /b 1)
"%JDK_BIN%\java.exe" -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp build\testes br.com.truco.TesteRegras
