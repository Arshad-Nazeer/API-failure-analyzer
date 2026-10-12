# API Failure Analyzer - Quick Launch Script
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Starting API Failure Analyzer App...   " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 1. Load backend/.env into current environment
$envFile = "$PSScriptRoot\backend\.env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $parts = $line.Split("=", 2)
            $k = $parts[0].Trim()
            $v = $parts[1].Trim()
            [System.Environment]::SetEnvironmentVariable($k, $v, "Process")
        }
    }
}

# 2. Check and clean up existing port 8080 if already bound
$port8080 = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($port8080) {
    Write-Host "Port 8080 is currently occupied by PID $($port8080.OwningProcess[0]). Reusing/Restarting..." -ForegroundColor Yellow
}

# 3. Start Backend in a separate window
$backendCmd = '$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"; cd "' + $PSScriptRoot + '\backend"; Write-Host "Starting Spring Boot Backend on http://localhost:8080..." -ForegroundColor Green; .\mvnw.cmd spring-boot:run'
Start-Process powershell -ArgumentList "-NoExit", "-Command", $backendCmd

# 4. Start Frontend in a separate window
$frontendCmd = 'cd "' + $PSScriptRoot + '\frontend"; Write-Host "Starting Frontend on http://localhost:5173..." -ForegroundColor Cyan; npm run dev'
Start-Process powershell -ArgumentList "-NoExit", "-Command", $frontendCmd

# 5. Open browser after short delay
Start-Sleep -Seconds 3
Start-Process "http://localhost:5173"

Write-Host "Both Backend (http://localhost:8080) and Frontend (http://localhost:5173) launched!" -ForegroundColor Yellow
Write-Host "Browser will open automatically." -ForegroundColor Yellow
