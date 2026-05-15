# ============================================================
#  MediBook — Stop All Services
#  Right-click → Run with PowerShell
# ============================================================

$Host.UI.RawUI.WindowTitle = "MediBook — Stopping Services"

Write-Host ""
Write-Host "=====================================================" -ForegroundColor Red
Write-Host "  MediBook Backend - Stopping All Services" -ForegroundColor Red
Write-Host "=====================================================" -ForegroundColor Red
Write-Host ""

$confirm = Read-Host "  Type YES to confirm stopping all MediBook services"
if ($confirm -ne "YES") {
    Write-Host "  Cancelled. No services were stopped." -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit
}

# Ports to kill
$ports = @(8761, 8081, 8082, 8083, 8084, 8085, 8086, 8087, 8088, 8089, 8080)
$names = @("eureka-server","auth-service","provider-service","schedule-service",
           "appointment-service","payment-service","review-service",
           "notification-service","record-service","admin-service","api-gateway")

Write-Host ""
Write-Host "  Killing processes by port..." -ForegroundColor Yellow
Write-Host ""

for ($i = 0; $i -lt $ports.Length; $i++) {
    $port = $ports[$i]
    $name = $names[$i]
    $result = netstat -ano | Select-String ":$port " | Select-String "LISTENING"
    if ($result) {
        $pid_ = ($result -split "\s+")[-1]
        try {
            Stop-Process -Id $pid_ -Force -ErrorAction SilentlyContinue
            Write-Host "  ✅ Stopped $name (port $port, PID $pid_)" -ForegroundColor Green
        } catch {
            Write-Host "  ⚠️  Could not stop $name on port $port" -ForegroundColor Yellow
        }
    } else {
        Write-Host "  ℹ️  $name (port $port) was not running" -ForegroundColor Gray
    }
}

# Also close the named windows
Write-Host ""
Write-Host "  Closing terminal windows..." -ForegroundColor Yellow
foreach ($name in $names) {
    $proc = Get-Process | Where-Object { $_.MainWindowTitle -like "*$name*" }
    if ($proc) {
        $proc | Stop-Process -Force -ErrorAction SilentlyContinue
        Write-Host "  ✅ Closed window: $name" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "=====================================================" -ForegroundColor Green
Write-Host "  All MediBook services stopped!" -ForegroundColor Green
Write-Host "=====================================================" -ForegroundColor Green
Write-Host ""
Read-Host "Press Enter to close"
