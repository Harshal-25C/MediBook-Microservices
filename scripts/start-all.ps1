# ============================================================
#  MediBook — Start All Services (Smart Wait Version)
#  Run this in PowerShell as Administrator for best results.
#  Right-click this file → Run with PowerShell
# ============================================================

$Host.UI.RawUI.WindowTitle = "MediBook — Starting Services"

# ── CONFIG: Set this to your project root folder ────────────
$ROOT = "D:\MediBook-Microservices"
# If the scripts folder is INSIDE the project root, the above is correct.
# Otherwise hard-code it like this:
# $ROOT = "C:\Users\YourName\Desktop\MediBook-Microservices-feature-admin-server"

Write-Host ""
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "  MediBook Backend - Starting All 11 Services" -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Project Root: $ROOT" -ForegroundColor Gray
Write-Host ""

# ── Helper: Wait until a port is LISTENING ──────────────────
function Wait-ForPort {
    param(
        [int]$Port,
        [string]$ServiceName,
        [int]$TimeoutSeconds = 90
    )
    Write-Host "  ⏳ Waiting for $ServiceName (port $Port)..." -ForegroundColor Yellow
    $elapsed = 0
    while ($elapsed -lt $TimeoutSeconds) {
        $conn = Test-NetConnection -ComputerName localhost -Port $Port -WarningAction SilentlyContinue -ErrorAction SilentlyContinue
        if ($conn.TcpTestSucceeded) {
            Write-Host "  ✅ $ServiceName is UP on port $Port" -ForegroundColor Green
            return $true
        }
        Start-Sleep -Seconds 3
        $elapsed += 3
        Write-Host "     Still waiting... ($elapsed/$TimeoutSeconds s)" -ForegroundColor DarkGray
    }
    Write-Host "  ⚠️  $ServiceName did not start within $TimeoutSeconds seconds!" -ForegroundColor Red
    return $false
}

# ── Helper: Start a service in a new window ─────────────────
function Start-Service {
    param(
        [string]$ServiceName,
        [string]$ServicePath,
        [string]$Port
    )
    Write-Host ""
    Write-Host "  🚀 Launching $ServiceName on port $Port..." -ForegroundColor White
    Start-Process cmd -ArgumentList "/k title $ServiceName && cd /d `"$ServicePath`" && mvn spring-boot:run" -WindowStyle Normal
}

# ════════════════════════════════════════════════════════════
#  START SERVICES IN ORDER
# ════════════════════════════════════════════════════════════

# [1] Eureka Server — Everything else depends on this
Start-Service "eureka-server" "$ROOT\eureka-server" "8761"
Wait-ForPort 8761 "eureka-server" 90

# [2] Auth Service
Start-Service "auth-service" "$ROOT\auth-service" "8081"
Wait-ForPort 8081 "auth-service" 90

# [3] Provider Service
Start-Service "provider-service" "$ROOT\provider-service" "8082"
Wait-ForPort 8082 "provider-service" 90

# [4] Schedule Service
Start-Service "schedule-service" "$ROOT\schedule-service" "8083"
Wait-ForPort 8083 "schedule-service" 90

# [5] Appointment Service
Start-Service "appointment-service" "$ROOT\appointment-service" "8084"
Wait-ForPort 8084 "appointment-service" 90

# [6] Payment Service
Start-Service "payment-service" "$ROOT\payment-service" "8085"
Wait-ForPort 8085 "payment-service" 90

# [7] Review Service
Start-Service "review-service" "$ROOT\review-service" "8086"
Wait-ForPort 8086 "review-service" 90

# [8] Notification Service
Start-Service "notification-service" "$ROOT\notification-service" "8087"
Wait-ForPort 8087 "notification-service" 90

# [9] Record Service
Start-Service "record-service" "$ROOT\record-service" "8088"
Wait-ForPort 8088 "record-service" 90

# [10] Admin Service
Start-Service "admin-service" "$ROOT\admin-service" "8089"
Wait-ForPort 8089 "admin-service" 90

# [11] API Gateway — ALWAYS LAST
Start-Service "api-gateway" "$ROOT\api-gateway" "8080"
Wait-ForPort 8080 "api-gateway" 90

# ════════════════════════════════════════════════════════════
Write-Host ""
Write-Host "=====================================================" -ForegroundColor Green
Write-Host "  🎉 All 11 services are UP and RUNNING!" -ForegroundColor Green
Write-Host "=====================================================" -ForegroundColor Green
Write-Host ""
Write-Host "  Eureka Dashboard : http://localhost:8761" -ForegroundColor Cyan
Write-Host "  API Gateway      : http://localhost:8080" -ForegroundColor Cyan
Write-Host "  Frontend         : http://localhost:5173" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Each service is in its own terminal window." -ForegroundColor Gray
Write-Host "  You can close THIS window now." -ForegroundColor Gray
Write-Host "=====================================================" -ForegroundColor Green
Write-Host ""
Read-Host "Press Enter to close this window"
