<#
.SYNOPSIS
    Builds a self-contained Windows application for GraphiCV using jpackage.

.DESCRIPTION
    Produces target/dist/GraphiCV/GraphiCV.exe -- a native launcher bundled with a
    trimmed Java runtime, so end users do not need Java installed.

    With -Installer, also produces a distributable installer (.exe or .msi).
    Installer creation requires the WiX Toolset v3 to be on the PATH
    (https://github.com/wixtoolset/wix3/releases). The default app-image build
    needs nothing beyond a JDK 17+.

.PARAMETER Installer
    Also build an installer package instead of just the app image.

.PARAMETER Type
    Installer type when -Installer is given: exe (default) or msi.

.EXAMPLE
    .\packaging\package-windows.ps1
    .\packaging\package-windows.ps1 -Installer -Type msi
#>
[CmdletBinding()]
param(
    [switch]$Installer,
    [ValidateSet('exe', 'msi')]
    [string]$Type = 'exe'
)

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$appName     = 'GraphiCV'
$appVersion  = '1.0'
$mainClass   = 'com.unhuman.graphicv.GraphiCV'
$jarName     = "$appName-$appVersion.jar"

$stageDir = Join-Path $projectRoot 'target\jpackage-input'
$distDir  = Join-Path $projectRoot 'target\dist'

# --- 1. Build the runnable jar -------------------------------------------------
Write-Host "==> Building $jarName with Maven..." -ForegroundColor Cyan
& mvn -f (Join-Path $projectRoot 'pom.xml') -q clean package
if ($LASTEXITCODE -ne 0) { throw "Maven build failed (exit $LASTEXITCODE)." }

$jarPath = Join-Path $projectRoot "target\$jarName"
if (-not (Test-Path $jarPath)) { throw "Expected jar not found: $jarPath" }

# --- 2. Stage just the jar for jpackage ---------------------------------------
if (Test-Path $stageDir) { Remove-Item -Recurse -Force $stageDir }
New-Item -ItemType Directory -Path $stageDir | Out-Null
Copy-Item $jarPath $stageDir

if (Test-Path $distDir) { Remove-Item -Recurse -Force $distDir }
New-Item -ItemType Directory -Path $distDir | Out-Null

# --- 3. Run jpackage -----------------------------------------------------------
$jpackage = (Get-Command jpackage -ErrorAction SilentlyContinue).Source
if (-not $jpackage) {
    throw "jpackage was not found on the PATH. Install a JDK 17 or newer and ensure its bin directory is on the PATH."
}

$jpackageArgs = @(
    '--name',        $appName
    '--app-version', $appVersion
    '--input',       $stageDir
    '--main-jar',    $jarName
    '--main-class',  $mainClass
    '--dest',        $distDir
    '--vendor',      'unHUman / Howard Uman'
    '--description', 'Graphics / Sprite Editor for ColecoVision and TI-99/4A'
)

if ($Installer) {
    $jpackageArgs += @(
        '--type',            $Type
        '--win-dir-chooser'
        '--win-menu'
        '--win-shortcut'
        '--win-shortcut-prompt'
    )
    Write-Host "==> Running jpackage (installer: $Type)..." -ForegroundColor Cyan
} else {
    $jpackageArgs += @('--type', 'app-image')
    Write-Host "==> Running jpackage (app-image)..." -ForegroundColor Cyan
}

$iconPath = Join-Path $PSScriptRoot 'GraphiCV.ico'
if (Test-Path $iconPath) { $jpackageArgs += @('--icon', $iconPath) }

& $jpackage @jpackageArgs
if ($LASTEXITCODE -ne 0) { throw "jpackage failed (exit $LASTEXITCODE)." }

Write-Host ""
Write-Host "Done. Output in: $distDir" -ForegroundColor Green
Get-ChildItem $distDir | ForEach-Object { Write-Host "  $($_.Name)" }
if (-not $Installer) {
    Write-Host ""
    Write-Host "Run it with: $distDir\$appName\$appName.exe"
}
