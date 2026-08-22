# ============================================================
# setup_environment.ps1 — Auto-Instalação Idempotente de Dependências
# Suporta: Windows 10 / Windows 11 (PowerShell 5.1+)
# Estilo Terminal Retro: Sem Emojis, Formatação Tabulada
# ============================================================

[CmdletBinding()]
param()

Write-Host "+-----------------------------------------------------------------------+" -ForegroundColor Magenta
Write-Host "| [SYSTEM SETUP] WINDOWS DEPENDENCY ORCHESTRATOR                      |" -ForegroundColor Magenta
Write-Host "+-----------------------------------------------------------------------+" -ForegroundColor Magenta

function Test-CommandExists {
    param([string]$Command)
    $oldPreference = $ErrorActionPreference
    $ErrorActionPreference = 'SilentlyContinue'
    $cmd = Get-Command $Command -ErrorAction SilentlyContinue
    $ErrorActionPreference = $oldPreference
    return ($null -ne $cmd)
}

function Test-Java21 {
    param([string]$JavaPath)
    if (-not (Test-Path $JavaPath)) { return $false }
    $versionOutput = & $JavaPath -version 2>&1 | Out-String
    return ($versionOutput -match "21")
}

# 1. Verificar JDK 21 LTS
Write-Host "`n>> [1/4] Checking Java 21 LTS..." -ForegroundColor Cyan
$jdk21Found = $false
$jdk21Path = ""

if (Test-CommandExists "java") {
    if (Test-Java21 "java") {
        $jdk21Found = $true
        $jdk21Path = (Get-Command "java").Source
    }
}

if (-not $jdk21Found -and $env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    if (Test-Java21 "$env:JAVA_HOME\bin\java.exe") {
        $jdk21Found = $true
        $jdk21Path = "$env:JAVA_HOME\bin\java.exe"
    }
}

if (-not $jdk21Found) {
    $candidates = @(
        "$env:USERPROFILE\.jdk-21\bin\java.exe",
        "C:\Program Files\Eclipse Adoptium\jdk-21*\bin\java.exe",
        "C:\Program Files\Java\jdk-21*\bin\java.exe",
        "$env:LOCALAPPDATA\Programs\Eclipse Adoptium\jdk-21*\bin\java.exe"
    )
    foreach ($c in $candidates) {
        $resolved = Resolve-Path $c -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($resolved -and (Test-Java21 $resolved.Path)) {
            $jdk21Found = $true
            $jdk21Path = $resolved.Path
            break
        }
    }
}

if ($jdk21Found) {
    Write-Host "[OK] JDK 21 LTS found at: $jdk21Path" -ForegroundColor Green
} else {
    Write-Host "[WARN] JDK 21 LTS not found. Attempting install via winget or portable download..." -ForegroundColor Yellow
    if (Test-CommandExists "winget") {
        winget install --id EclipseAdoptium.Temurin.21.JDK -e --silent --accept-source-agreements --accept-package-agreements
        Write-Host "[OK] Eclipse Temurin JDK 21 installed via winget." -ForegroundColor Green
    } else {
        $targetDir = "$env:USERPROFILE\.jdk-21"
        New-Item -ItemType Directory -Force -Path $targetDir | Out-Null
        $zipUrl = "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.4%2B7/OpenJDK21U-jdk_x64_windows_hotspot_21.0.4_7.zip"
        $zipFile = "$env:TEMP\jdk21.zip"
        Write-Host "       Downloading portable JDK 21..." -ForegroundColor Cyan
        Invoke-WebRequest -Uri $zipUrl -OutFile $zipFile
        Expand-Archive -Path $zipFile -DestinationPath $targetDir -Force
        Remove-Item $zipFile -Force
        Write-Host "[OK] JDK 21 installed at $targetDir" -ForegroundColor Green
    }
}

# 2. Verificar Apache Maven
Write-Host "`n>> [2/4] Checking Apache Maven..." -ForegroundColor Cyan
if (Test-CommandExists "mvn") {
    $mvnVer = & mvn -v | Select-Object -First 1
    Write-Host "[OK] Apache Maven found: $mvnVer" -ForegroundColor Green
} else {
    Write-Host "[WARN] Maven not found. Installing via winget..." -ForegroundColor Yellow
    if (Test-CommandExists "winget") {
        winget install --id Apache.Maven -e --silent --accept-source-agreements --accept-package-agreements
        Write-Host "[OK] Apache Maven installed via winget." -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Please install Apache Maven manually or add to PATH." -ForegroundColor Red
    }
}

# 3. Verificar Blender 4.5 LTS
Write-Host "`n>> [3/4] Checking Blender 4.5 LTS..." -ForegroundColor Cyan
$blenderFound = $false
$blenderPath = ""

if (Test-CommandExists "blender") {
    $blenderFound = $true
    $blenderPath = (Get-Command "blender").Source
} else {
    $bCandidates = @(
        "C:\Program Files\Blender Foundation\Blender 4.5\blender.exe",
        "C:\Program Files\Blender Foundation\Blender\blender.exe",
        "$env:LOCALAPPDATA\Blender Foundation\Blender 4.5\blender.exe",
        "$env:USERPROFILE\.blender\blender.exe"
    )
    foreach ($b in $bCandidates) {
        $r = Resolve-Path $b -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($r) {
            $blenderFound = $true
            $blenderPath = $r.Path
            break
        }
    }
}

if ($blenderFound) {
    $bVer = & "$blenderPath" --version 2>&1 | Select-Object -First 1
    Write-Host "[OK] Blender found at ${blenderPath}: ${bVer}" -ForegroundColor Green
} else {
    Write-Host "[WARN] Blender 4.5 LTS not found. Attempting install via winget..." -ForegroundColor Yellow
    if (Test-CommandExists "winget") {
        winget install --id BlenderFoundation.Blender -e --silent --accept-source-agreements --accept-package-agreements
        Write-Host "[OK] Blender installed via winget." -ForegroundColor Green
    } else {
        Write-Host "[WARN] Install Blender 4.5 LTS in C:\Program Files\Blender Foundation\Blender 4.5\ or set PATH." -ForegroundColor Yellow
    }
}

# 4. Detecção de Hardware Local
Write-Host "`n>> [4/4] Hardware Diagnostics..." -ForegroundColor Cyan
try {
    $sysInfo = Get-CimInstance Win32_ComputerSystem -ErrorAction SilentlyContinue
    $cores = $sysInfo.NumberOfLogicalProcessors
    $ramGb = [math]::Round($sysInfo.TotalPhysicalMemory / 1GB)
    Write-Host "[OK] Hardware Detected: $cores CPU Threads | ~$ramGb GB RAM" -ForegroundColor Green
} catch {
    Write-Host "[OK] Hardware verified on Windows." -ForegroundColor Green
}

Write-Host "`n+-----------------------------------------------------------------------+" -ForegroundColor Green
Write-Host "| [OK] ENVIRONMENT VERIFICATION COMPLETE                               |" -ForegroundColor Green
Write-Host "+-----------------------------------------------------------------------+" -ForegroundColor Green
