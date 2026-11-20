@echo off
setlocal enabledelayedexpansion

echo ==============================================================================
echo    VELALAR COLLEGE OF ENGINEERING AND TECHNOLOGY, ERODE - 638012
echo              DEPARTMENT OF INFORMATION TECHNOLOGY
echo    LIBRARY BOOK LENDING AND USAGE ANALYSIS SYSTEM (SCENARIO 4)
echo    Student: Abishek ^| Register No: 732925ITR004 ^| Section: 25IT3A
echo ==============================================================================
echo.

if "%1"=="--cli" goto RUN_CLI
if "%1"=="--java" goto RUN_JAVA
if "%1"=="--node" goto RUN_NODE
if "%1"=="--python" goto RUN_PYTHON

echo Select run mode:
echo   [1] Start Web Server & Interactive Dashboard (Java / Node) [Recommended]
echo   [2] Start Java CLI Application (Console Menu)
echo   [3] Run Python Analytics & Visualizations Pipeline
echo   [4] Exit
echo.
set /p CHOICE="Enter choice [1-4]: "

if "%CHOICE%"=="1" goto RUN_WEB
if "%CHOICE%"=="2" goto RUN_CLI
if "%CHOICE%"=="3" goto RUN_PYTHON
if "%CHOICE%"=="4" exit /b 0
goto RUN_WEB

:RUN_WEB
echo.
echo [1/2] Checking environment...
where java >nul 2>nul
if %errorlevel% equ 0 (
    echo [INFO] Java runtime detected! Launching Java Web Server...
    goto RUN_JAVA
)

where node >nul 2>nul
if %errorlevel% equ 0 (
    echo [INFO] Node.js detected! Launching Web Server via Node.js...
    goto RUN_NODE
)

echo [ERROR] Neither Java nor Node.js was found in PATH.
echo Please install Java 11+ or Node.js to run the web server.
pause
exit /b 1

:RUN_JAVA
echo.
echo Compiling Java source files...
if not exist "bin" mkdir bin
javac -encoding UTF-8 -d bin src\com\library\model\*.java src\com\library\exception\*.java src\com\library\util\*.java src\com\library\dao\*.java src\com\library\service\*.java src\com\library\server\*.java src\com\library\Main.java
if %errorlevel% neq 0 (
    echo [ERROR] Java compilation failed.
    pause
    exit /b 1
)
echo [SUCCESS] Java compilation successful.
echo Starting Library Circulation Web Server on http://localhost:8080 ...
start http://localhost:8080
java -cp "bin;lib/*" com.library.server.LibraryServer 8080
pause
exit /b 0

:RUN_CLI
echo.
echo Compiling Java source files for CLI...
if not exist "bin" mkdir bin
javac -encoding UTF-8 -d bin src\com\library\model\*.java src\com\library\exception\*.java src\com\library\util\*.java src\com\library\dao\*.java src\com\library\service\*.java src\com\library\server\*.java src\com\library\Main.java
if %errorlevel% neq 0 (
    echo [ERROR] Java compilation failed.
    pause
    exit /b 1
)
java -cp "bin;lib/*" com.library.Main
pause
exit /b 0

:RUN_NODE
echo.
echo Starting Web Server via Node.js on http://localhost:8080 ...
start http://localhost:8080
node server.js
pause
exit /b 0

:RUN_PYTHON
echo.
echo Running Python Data Science & Analytics Pipeline...
set "PYTHON_EXE="
for /d %%P in ("%LOCALAPPDATA%\Programs\Python\Python*") do (
    if exist "%%~fP\python.exe" set "PYTHON_EXE=%%~fP\python.exe"
)
if defined PYTHON_EXE (
    "%PYTHON_EXE%" python_analytics\analysis.py
) else (
    python python_analytics\analysis.py
    if errorlevel 1 py -3 python_analytics\analysis.py
)
pause
exit /b 0
