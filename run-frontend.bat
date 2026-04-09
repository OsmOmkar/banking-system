@echo off
REM ============================================================
REM JavaBank — Frontend Local Server (Windows)
REM Serves the frontend at http://localhost:3000
REM Run AFTER run-local.bat (in a separate window)
REM ============================================================
title JavaBank Frontend Server

echo.
echo  ========================================
echo   JavaBank - Frontend Server
echo  ========================================
echo.

cd /d "%~dp0frontend"

echo  Trying Python (http.server)...
python --version >nul 2>&1
if %errorlevel% == 0 (
    echo.
    echo  ┌─────────────────────────────────────────┐
    echo  │  Frontend:   http://localhost:3000       │
    echo  │  Open this in your browser!             │
    echo  │                                         │
    echo  │  Press Ctrl+C to stop                  │
    echo  └─────────────────────────────────────────┘
    echo.
    python -m http.server 3000
    goto done
)

python3 --version >nul 2>&1
if %errorlevel% == 0 (
    echo.
    echo  Frontend: http://localhost:3000
    python3 -m http.server 3000
    goto done
)

echo  Python not found. Trying Node.js (npx serve)...
node --version >nul 2>&1
if %errorlevel% == 0 (
    echo.
    echo  Frontend: http://localhost:3000
    npx -y serve -s . -l 3000
    goto done
)

echo.
echo  =========================================================
echo   ERROR: Neither Python nor Node.js found!
echo   You can still open the frontend directly:
echo   Just open this file in your browser:
echo   %~dp0frontend\index.html
echo  =========================================================
echo.
echo  If the browser blocks requests (CORS), install Python:
echo  https://www.python.org/downloads/
echo.

:done
pause
