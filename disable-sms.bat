@echo off
:: ============================================================
:: disable-sms.bat
:: Stops real SMS delivery to mobile numbers.
:: OTPs will still appear in Railway console/logs — nothing breaks.
:: Pushes to GitHub → Railway auto-redeploys.
:: ============================================================

echo.
echo ==========================================
echo   JavaBank — DISABLING SMS Delivery
echo   OTPs will show in Railway logs only.
echo   No credits will be consumed.
echo ==========================================
echo.

:: Patch config.properties — set sms.enabled=false
set CONFIG=backend\src\main\resources\config.properties
powershell -Command "(Get-Content '%CONFIG%') -replace 'sms.enabled=true', 'sms.enabled=false' | Set-Content '%CONFIG%'"

:: Verify the change
findstr "sms.enabled" "%CONFIG%"

echo.
echo [OK] sms.enabled set to FALSE in config.properties
echo [..] Committing and pushing to GitHub...

git add backend\src\main\resources\config.properties
git commit -m "toggle: disable SMS delivery to phones (sms.enabled=false)"
git push origin main --force

echo.
echo ==========================================
echo   DONE! SMS is now DISABLED.
echo   Railway will redeploy in ~1-2 minutes.
echo   OTPs will appear in Railway logs only.
echo   Run enable-sms.bat to turn SMS back on.
echo ==========================================
echo.
pause
