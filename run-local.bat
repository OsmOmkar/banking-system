@echo off
REM ============================================================
REM JavaBank — Local Development Runner (Windows)
REM Double-click this file OR run in Command Prompt / PowerShell
REM ============================================================
title JavaBank Local Server

echo.
echo  ========================================
echo   JavaBank - Local Development Server
echo  ========================================
echo.

REM Change to backend directory
cd /d "%~dp0backend"

REM ── Step 0: Kill any existing server on port 8081 ─────────
echo Checking for existing server on port 8081...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$p = netstat -ano | Select-String ':8081 ' | ForEach-Object { ($_ -split '\s+')[-1] } | Select-Object -Unique; if ($p) { $p | ForEach-Object { taskkill /PID $_ /F 2>$null }; Write-Host 'Old server stopped.' } else { Write-Host 'Port 8081 is free.' }"
timeout /t 2 /nobreak >nul

REM ── Step 1: Create output directories ──────────────────────
if not exist "out" mkdir out
if not exist "lib" mkdir lib

REM ── Step 2: Download PostgreSQL JDBC driver if missing ─────
if not exist "lib\postgresql.jar" (
    echo [1/4] Downloading PostgreSQL JDBC driver...
    powershell -Command "Invoke-WebRequest -Uri 'https://jdbc.postgresql.org/download/postgresql-42.7.3.jar' -OutFile 'lib\postgresql.jar' -UseBasicParsing"
    if errorlevel 1 (
        echo ERROR: Failed to download JDBC driver. Check your internet connection.
        pause
        exit /b 1
    )
    echo       Downloaded successfully!
) else (
    echo [1/4] JDBC driver already present - skipping download.
)
echo.

REM ── Step 3: Compile all Java sources ───────────────────────
echo [2/4] Compiling Java source files...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$files = (Get-ChildItem -Recurse -Filter '*.java' -Path 'src\main\java').FullName; & javac -encoding UTF-8 -cp 'lib\postgresql.jar' -d 'out' $files; exit $LASTEXITCODE"
if errorlevel 1 (
    echo.
    echo =========================================
    echo  ERROR: Compilation FAILED!
    echo  Check the errors above and fix them.
    echo =========================================
    pause
    exit /b 1
)
echo       Compilation successful!
echo.

REM ── Step 4: Copy resources (config.properties) ─────────────
echo [3/4] Copying resources...
xcopy /E /Y "src\main\resources\*" "out\" >nul 2>&1
echo       Resources copied!
echo.

REM ── Step 5: Start the server ───────────────────────────────
echo [4/4] Starting JavaBank backend server...
echo.
echo  +------------------------------------------+
echo  ^|  Backend API:   http://localhost:8081     ^|
echo  ^|  Health check:  http://localhost:8081/api/health  ^|
echo  ^|                                           ^|
echo  ^|  Press Ctrl+C to stop the server         ^|
echo  +------------------------------------------+
echo.
echo  NOTE: Keep this window open!
echo  Open a NEW window and run: run-frontend.bat
echo  OR open frontend/index.html in your browser.
echo.

set PORT=8081
java -cp "out;lib\postgresql.jar" com.banking.BankingServer

echo.
echo Server stopped.
pause
