# ============================================
# restart-backend.ps1
# Kills any process on port 8081, then starts Spring Boot
# Usage: .\restart-backend.ps1
# ============================================

Write-Host "Checking port 8081..." -ForegroundColor Cyan

$portInfo = netstat -ano | Select-String ":8081 " | Select-String "LISTENING"
if ($portInfo) {
    $pid8081 = ($portInfo -split '\s+')[-1]
    Write-Host "Killing process PID $pid8081 on port 8081..." -ForegroundColor Yellow
    taskkill /PID $pid8081 /F
    Start-Sleep -Seconds 2
    Write-Host "Process killed." -ForegroundColor Green
} else {
    Write-Host "Port 8081 is free." -ForegroundColor Green
}

Write-Host ""
Write-Host "Starting Spring Boot backend..." -ForegroundColor Cyan
Set-Location "$PSScriptRoot\resume-parser-backend"
mvn spring-boot:run
