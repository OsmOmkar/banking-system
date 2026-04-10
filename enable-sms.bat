@echo off
:: ============================================================
:: enable-sms.bat
:: Activates real SMS delivery to registered mobile numbers.
:: Charges Rs.5 per message from Fast2SMS balance.
:: Pushes to GitHub → Railway auto-redeploys.
:: ============================================================

echo.
echo ==========================================
echo   JavaBank — ENABLING SMS Delivery
echo   Rs.5 will be charged per SMS sent.
echo ==========================================
echo.

:: Patch config.properties — set sms.enabled=true
set CONFIG=backend\src\main\resources\config.properties
powershell -Command "(Get-Content '%CONFIG%') -replace 'sms.enabled=false', 'sms.enabled=true' | Set-Content '%CONFIG%'"

:: Verify the change
findstr "sms.enabled" "%CONFIG%"

echo.
echo [OK] sms.enabled set to TRUE in config.properties
echo [..] Committing and pushing to GitHub...

git add backend\src\main\resources\config.properties
git commit -m "toggle: enable real SMS delivery (sms.enabled=true)"
git push origin main --force

echo.
echo ==========================================
echo   DONE! SMS is now ENABLED.
echo   Railway will redeploy in ~1-2 minutes.
echo   OTPs will be sent to registered phones.
echo ==========================================
echo.
pause
