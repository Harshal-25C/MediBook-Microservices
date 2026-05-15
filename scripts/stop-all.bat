@echo off
title MediBook - Stopping All Services
color 0C

echo.
echo  =====================================================
echo   MediBook Backend - Stopping All Services
echo  =====================================================
echo.
set /p CONFIRM=  Type YES to stop all MediBook services: 

if /i not "%CONFIRM%"=="YES" (
    echo  Cancelled.
    pause
    exit /b
)

echo.
echo  [INFO] Killing MediBook services by port...
echo.

for %%P in (8761 8081 8082 8083 8084 8085 8086 8087 8088 8089 8080) do (
    for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":%%P " ^| findstr "LISTENING" 2^>nul') do (
        taskkill /PID %%a /F >nul 2>&1
        echo  [STOPPED] Port %%P - PID %%a killed
    )
)

echo.
echo  [INFO] Closing service terminal windows...
for %%W in (eureka-server auth-service provider-service schedule-service appointment-service payment-service review-service notification-service record-service admin-service api-gateway) do (
    taskkill /FI "WINDOWTITLE eq %%W" /F >nul 2>&1
)

echo.
echo  =====================================================
echo   All MediBook services stopped!
echo  =====================================================
echo.
pause
