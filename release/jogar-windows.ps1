$ErrorActionPreference = 'Stop'
$releaseDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$jar = Get-ChildItem -Path $releaseDir -Filter 'apsu-game-v*-windows-x64.jar' |
    Sort-Object Name -Descending | Select-Object -First 1
if ($jar -and $jar.Name -match '^apsu-game-(v\d{8}-\d{6})-windows-x64\.jar$') {
    $releaseTag = $Matches[1]
}
$resources = if ($releaseTag) { Get-Item -Path (Join-Path $releaseDir "apsu-game-$releaseTag-resources.zip") -ErrorAction SilentlyContinue }
if (-not $jar -or -not $resources) {
    throw 'Coloque nesta pasta o JAR Windows e o arquivo de recursos da mesma release.'
}

function Get-JavaMajor([string]$Path) {
    if (-not $Path -or -not (Test-Path -LiteralPath $Path)) { return 0 }
    $output = (& $Path -version 2>&1 | Out-String)
    if ($output -match 'version "(\d+)') { return [int]$Matches[1] }
    return 0
}

$javaCommand = Get-Command java.exe -ErrorAction SilentlyContinue | Select-Object -First 1
$java = if ($javaCommand) { $javaCommand.Source } else { $null }
if ((Get-JavaMajor $java) -lt 21) {
    $runtimeRoot = Join-Path $env:LOCALAPPDATA 'ApsuGame\runtimes\windows-x64-temurin-21'
    $cachedJava = Join-Path $runtimeRoot 'bin\java.exe'
    if ((Get-JavaMajor $cachedJava) -lt 21) {
        $temp = Join-Path $env:TEMP ('apsu-java-' + [guid]::NewGuid().ToString('N'))
        $zip = "$temp.zip"
        New-Item -ItemType Directory -Force -Path $temp | Out-Null
        try {
            Write-Host 'Baixando Java 21 uma única vez para sua conta de usuário...'
            $url = 'https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jre/hotspot/normal/eclipse'
            Invoke-WebRequest -Uri $url -OutFile $zip
            Expand-Archive -Path $zip -DestinationPath $temp -Force
            $downloadedJava = Get-ChildItem -Path $temp -Filter java.exe -Recurse | Select-Object -First 1
            if (-not $downloadedJava) { throw 'O arquivo do Java não contém java.exe.' }
            $javaHome = Split-Path -Parent (Split-Path -Parent $downloadedJava.FullName)
            if (Test-Path $runtimeRoot) { Remove-Item -Path $runtimeRoot -Recurse -Force }
            New-Item -ItemType Directory -Force -Path (Split-Path -Parent $runtimeRoot) | Out-Null
            Move-Item -Path $javaHome -Destination $runtimeRoot
        } finally {
            Remove-Item -Path $temp -Recurse -Force -ErrorAction SilentlyContinue
            Remove-Item -Path $zip -Force -ErrorAction SilentlyContinue
        }
    }
    $java = $cachedJava
}

$classpath = "$($jar.FullName);$($resources.FullName)"
& $java -cp $classpath br.apsu.Launcher
exit $LASTEXITCODE
