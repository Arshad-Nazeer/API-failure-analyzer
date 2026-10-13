# ==============================================================================
# API Failure Analyzer - Dedicated Frontend Launcher
# ==============================================================================

$Host.UI.RawUI.WindowTitle = "API Failure Analyzer - Frontend (Vite :5173)"

$scriptDir = $PSScriptRoot
$frontendDir = Join-Path $scriptDir "frontend"
Set-Location $frontendDir

Write-Host "===========================================================" -ForegroundColor Cyan
Write-Host " API Failure Analyzer - Frontend Dev Server (:5173)        " -ForegroundColor Cyan
Write-Host " Starting Vite server...                                   " -ForegroundColor Cyan
Write-Host "===========================================================" -ForegroundColor Cyan

npm run dev
