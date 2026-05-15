@echo off
REM ══════════════════════════════════════════════════════════════
REM  MediBook — Step 1: Build all JARs, Step 2: Docker up
REM  Run from: D:\MediBook-Microservices\
REM ══════════════════════════════════════════════════════════════

echo.
echo ============================================
echo  STEP 1: Building all JARs with Maven
echo ============================================
echo.

set SERVICES=eureka-server auth-service admin-service provider-service schedule-service appointment-service payment-service review-service notification-service record-service api-gateway

for %%s in (%SERVICES%) do (
    echo [Maven] Building %%s...
    cd %%s
    call mvn clean package -DskipTests -q
    if errorlevel 1 (
        echo.
        echo FAILED: %%s — Maven build error
        cd ..
        pause
        exit /b 1
    )
    cd ..
    echo [Maven] DONE: %%s
    echo.
)

echo ============================================
echo  STEP 2: Starting Docker containers
echo ============================================
echo.

docker-compose down -v
docker-compose up --build -d

echo.
echo ============================================
echo  All done! Watching logs (Ctrl+C to stop)
echo ============================================
docker-compose logs -f
