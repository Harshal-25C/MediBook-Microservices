@echo off
title MediBook - Starting All Services
color 0A

echo.
echo  =====================================================
echo   MediBook Backend - Starting All Services
echo  =====================================================
echo.

:: ── PROJECT ROOT ─────────────────────────────────────────────────────────────
set ROOT=D:\MediBook-Microservices

echo [INFO] Project root: %ROOT%
echo.

:: ════════════════════════════════════════════════════════════════════════════
::  MAVEN - Hardcoded to your installed path + fallback checks
:: ════════════════════════════════════════════════════════════════════════════
set MVN=

:: Your actual Maven installation (found from mvn --version output)
if exist "C:\Maven\apache-maven-3.9.15\bin\mvn.cmd" (
    set MVN=C:\Maven\apache-maven-3.9.15\bin\mvn.cmd
    echo [INFO] Maven found: C:\Maven\apache-maven-3.9.15\bin\mvn.cmd
    goto :mvn_found
)

:: Fallback: check if mvn is on system PATH
where mvn >nul 2>&1
if %errorlevel%==0 (
    set MVN=mvn
    echo [INFO] Maven found on system PATH.
    goto :mvn_found
)

:: Fallback: STS4 bundled Maven
for /d %%D in ("%USERPROFILE%\.sts4\*") do (
    for /d %%P in ("%%D\plugins\org.apache.maven.*") do (
        if exist "%%P\bin\mvn.cmd" (
            set MVN=%%P\bin\mvn.cmd
            echo [INFO] Maven found in STS: %%P\bin\mvn.cmd
            goto :mvn_found
        )
    )
)

:: Fallback: other common paths
if exist "C:\Maven\bin\mvn.cmd"              set MVN=C:\Maven\bin\mvn.cmd              && goto :mvn_found
if exist "C:\maven\bin\mvn.cmd"              set MVN=C:\maven\bin\mvn.cmd              && goto :mvn_found
if exist "C:\Program Files\Maven\bin\mvn.cmd" set MVN=C:\Program Files\Maven\bin\mvn.cmd && goto :mvn_found
if exist "D:\maven\bin\mvn.cmd"              set MVN=D:\maven\bin\mvn.cmd              && goto :mvn_found

color 0C
echo.
echo  [ERROR] Maven not found. Your Maven is at:
echo  C:\Maven\apache-maven-3.9.15\bin\mvn.cmd
echo  Check if that folder still exists.
echo.
pause
exit /b 1

:mvn_found
echo [INFO] Using: %MVN%
echo.

:: ════════════════════════════════════════════════════════════════════════════
::  START SERVICES IN ORDER
:: ════════════════════════════════════════════════════════════════════════════

echo [1/11] Starting eureka-server (port 8761)...
start "eureka-server" cmd /k "cd /d "%ROOT%\eureka-server" && "%MVN%" spring-boot:run"
echo [INFO] Waiting 25s for Eureka to fully boot...
timeout /t 25 /nobreak > nul

echo [2/11] Starting auth-service (port 8081)...
start "auth-service" cmd /k "cd /d "%ROOT%\auth-service" && "%MVN%" spring-boot:run"
timeout /t 10 /nobreak > nul

echo [3/11] Starting provider-service (port 8082)...
start "provider-service" cmd /k "cd /d "%ROOT%\provider-service" && "%MVN%" spring-boot:run"
timeout /t 10 /nobreak > nul

echo [4/11] Starting schedule-service (port 8083)...
start "schedule-service" cmd /k "cd /d "%ROOT%\schedule-service" && "%MVN%" spring-boot:run"
timeout /t 10 /nobreak > nul

echo [5/11] Starting appointment-service (port 8084)...
start "appointment-service" cmd /k "cd /d "%ROOT%\appointment-service" && "%MVN%" spring-boot:run"
timeout /t 10 /nobreak > nul

echo [6/11] Starting payment-service (port 8085)...
start "payment-service" cmd /k "cd /d "%ROOT%\payment-service" && "%MVN%" spring-boot:run"
timeout /t 10 /nobreak > nul

echo [7/11] Starting review-service (port 8086)...
start "review-service" cmd /k "cd /d "%ROOT%\review-service" && "%MVN%" spring-boot:run"
timeout /t 10 /nobreak > nul

echo [8/11] Starting notification-service (port 8087)...
start "notification-service" cmd /k "cd /d "%ROOT%\notification-service" && "%MVN%" spring-boot:run"
timeout /t 10 /nobreak > nul

echo [9/11] Starting record-service (port 8088)...
start "record-service" cmd /k "cd /d "%ROOT%\record-service" && "%MVN%" spring-boot:run"
timeout /t 10 /nobreak > nul

echo [10/11] Starting admin-service (port 8089)...
start "admin-service" cmd /k "cd /d "%ROOT%\admin-service" && "%MVN%" spring-boot:run"
timeout /t 12 /nobreak > nul

echo [11/11] Starting api-gateway (port 8080) - LAST...
start "api-gateway" cmd /k "cd /d "%ROOT%\api-gateway" && "%MVN%" spring-boot:run"

echo.
echo  =====================================================
echo   All 11 services launched!
echo.
echo   Eureka Dashboard : http://localhost:8761
echo   API Gateway      : http://localhost:8080
echo   Frontend         : http://localhost:5173
echo.
echo   Wait ~60s for all services to fully initialize.
echo   Check Eureka to confirm all are UP (green).
echo  =====================================================
echo.
pause
