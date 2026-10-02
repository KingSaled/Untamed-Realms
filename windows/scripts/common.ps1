# Shared helpers for the Untamed Realms Windows scripts. Needs only Windows 10/11 + PowerShell 5.1.
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'   # Invoke-WebRequest is extremely slow with the progress bar on
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$Root = Split-Path -Parent $PSScriptRoot
$NeoForgeVersion = ([regex]'neoforge\s*=\s*"([^"]+)"').Match((Get-Content "$Root\pack\pack.toml" -Raw)).Groups[1].Value
$MinecraftVersion = ([regex]'minecraft\s*=\s*"([^"]+)"').Match((Get-Content "$Root\pack\pack.toml" -Raw)).Groups[1].Value

$ReleaseBase = 'https://github.com/KingSaled/Untamed-Realms/releases/download/latest-test-build'

function Write-Step([string]$msg) { Write-Host ""; Write-Host "==> $msg" -ForegroundColor Cyan }
function Write-Done([string]$msg) { Write-Host $msg -ForegroundColor Green }

function Invoke-Download([string]$url, [string]$dest) {
    for ($i = 1; $i -le 3; $i++) {
        try { Invoke-WebRequest -Uri $url -OutFile $dest -UseBasicParsing; return }
        catch { if ($i -eq 3) { throw "Download failed: $url`n$($_.Exception.Message)" }; Start-Sleep -Seconds (2 * $i) }
    }
}

# Runs a program without PowerShell treating its stderr output as an error. Returns the exit code.
function Invoke-Native([string]$exe, [string[]]$arguments) {
    $old = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try { & $exe @arguments 2>&1 | ForEach-Object { Write-Host "    $_" } ; return $LASTEXITCODE }
    finally { $ErrorActionPreference = $old }
}

# Finds Java 21+, or downloads a private copy (Eclipse Temurin 21) next to these scripts. Never touches system Java.
function Get-Java {
    $local = Join-Path $Root 'runtime\bin\java.exe'
    if (Test-Path $local) { return $local }
    $cmd = Get-Command java -ErrorAction SilentlyContinue
    if ($cmd) {
        $ver = cmd /c "`"$($cmd.Source)`" -version 2>&1" | Out-String
        if ($ver -match 'version "(\d+)' -and [int]$Matches[1] -ge 21) { return $cmd.Source }
    }
    Write-Step "Downloading Java 21 (one time only, about 45 MB)"
    $zip = Join-Path $env:TEMP 'untamed-realms-java21.zip'
    Invoke-Download 'https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jre/hotspot/normal/eclipse?project=jdk' $zip
    $tmp = Join-Path $Root 'runtime-download'
    if (Test-Path $tmp) { Remove-Item $tmp -Recurse -Force }
    Expand-Archive -Path $zip -DestinationPath $tmp -Force
    $inner = Get-ChildItem $tmp -Directory | Select-Object -First 1
    if (Test-Path (Join-Path $Root 'runtime')) { Remove-Item (Join-Path $Root 'runtime') -Recurse -Force }
    Move-Item $inner.FullName (Join-Path $Root 'runtime')
    Remove-Item $tmp -Recurse -Force
    Remove-Item $zip -Force
    return $local
}

function Get-NeoForgeInstaller {
    $path = Join-Path $env:TEMP "neoforge-$NeoForgeVersion-installer.jar"
    if (-not (Test-Path $path)) {
        Write-Step "Downloading NeoForge $NeoForgeVersion"
        Invoke-Download "https://maven.neoforged.net/releases/net/neoforged/neoforge/$NeoForgeVersion/neoforge-$NeoForgeVersion-installer.jar" $path
    }
    return $path
}

function Test-FileHash([string]$file, [string]$format, [string]$expected) {
    if (-not $format -or -not $expected) { return $true }
    $algo = switch ($format.ToLower()) { 'sha1' { 'SHA1' } 'sha256' { 'SHA256' } 'sha512' { 'SHA512' } 'md5' { 'MD5' } default { $null } }
    if (-not $algo) { return $true }
    return ((Get-FileHash -Path $file -Algorithm $algo).Hash.ToLower() -eq $expected.ToLower())
}

# Installs every mod of the pack meant for $Side ('client' or 'server') into $ModsDir, plus our ur-*.jar mods.
# Only files this script installed are ever removed, so mods you add yourself are left alone.
function Sync-Mods([string]$ModsDir, [string]$Side) {
    New-Item -ItemType Directory -Force -Path $ModsDir | Out-Null
    $manifest = Join-Path $ModsDir '.untamed-realms-managed.txt'
    $previous = @(); if (Test-Path $manifest) { $previous = @(Get-Content $manifest) }
    $wanted = New-Object System.Collections.Generic.List[string]

    $entries = Get-ChildItem (Join-Path $Root 'pack\mods') -Filter '*.pw.toml'
    $n = 0
    foreach ($entry in $entries) {
        $n++
        $toml = Get-Content $entry.FullName -Raw
        $get = { param($key) ([regex]"(?m)^$key\s*=\s*`"([^`"]*)`"").Match($toml).Groups[1].Value }
        $modSide = & $get 'side'
        if ($modSide -and $modSide -ne 'both' -and $modSide -ne $Side) { continue }
        $file = & $get 'filename'; $url = & $get 'url'; $hashFormat = & $get 'hash-format'; $hash = & $get 'hash'
        if (-not $url -or -not $file) { Write-Warning "No direct download for $($entry.Name) - skipped"; continue }
        # PowerShell treats [ ] in paths as wildcards (e.g. "[Neoforge]ctov-3.6.3.jar"), so drop them locally
        $file = $file -replace '[\[\]]', ''
        $wanted.Add($file)
        $dest = Join-Path $ModsDir $file
        if ((Test-Path $dest) -and (Test-FileHash $dest $hashFormat $hash)) { continue }
        Write-Host ("  [{0}/{1}] {2}" -f $n, $entries.Count, $file)
        Invoke-Download $url $dest
        if (-not (Test-FileHash $dest $hashFormat $hash)) { Remove-Item $dest -Force; throw "Checksum mismatch for $file - please run again." }
    }
    foreach ($old in $previous) {
        if (-not $wanted.Contains($old)) { Remove-Item (Join-Path $ModsDir $old) -Force -ErrorAction SilentlyContinue }
    }
    Get-ChildItem $ModsDir -Filter 'ur-*.jar' | Remove-Item -Force
    foreach ($jar in Get-ChildItem (Join-Path $Root 'ur-mods') -Filter '*.jar') {
        Copy-Item $jar.FullName $ModsDir -Force
        $wanted.Add($jar.Name)
    }
    Set-Content -Path $manifest -Value $wanted
    Write-Done "  $($wanted.Count) mods ready in $ModsDir"
}

function Copy-PackConfig([string]$GameDir) {
    foreach ($folder in 'config', 'defaultconfigs') {
        $src = Join-Path $Root "pack\$folder"
        if (Test-Path $src) { Copy-Item $src -Destination $GameDir -Recurse -Force }
    }
}

function Get-RamGB { [math]::Round((Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory / 1GB) }

function Write-Utf8NoBom([string]$path, [string]$text) {
    [IO.File]::WriteAllText($path, $text, (New-Object Text.UTF8Encoding $false))
}

# Checks for a newer test build and, if there is one, updates this folder in place. Only the bundle's
# own files are replaced (scripts, pack, ur-mods); the server folder with your world, the downloaded
# Java and your settings are kept. Returns $true if files changed, so the caller can restart itself.
function Update-Bundle {
    if ($env:UR_NO_UPDATE -eq '1') { return $false }
    Write-Step "Checking for updates"
    $versionFile = Join-Path $Root 'VERSION.txt'
    $local = if (Test-Path $versionFile) { (Get-Content $versionFile -TotalCount 1).Trim() } else { '' }
    try {
        $response = Invoke-WebRequest -Uri "$ReleaseBase/version.txt" -UseBasicParsing -TimeoutSec 20
        $content = $response.Content
        if ($content -is [byte[]]) { $content = [Text.Encoding]::UTF8.GetString($content) }
        $latest = ($content -split "`r?`n")[0].Trim()
    } catch {
        Write-Host "  Could not check for updates (offline?). Continuing with this version." -ForegroundColor DarkGray
        return $false
    }
    if (-not $latest -or $latest -eq $local) { Write-Done "  You have the latest version."; return $false }

    Write-Step "Downloading the latest Untamed Realms build"
    $tmp = Join-Path ([IO.Path]::GetTempPath()) 'untamed-realms-update'
    Remove-Item $tmp -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path $tmp | Out-Null
    $zip = Join-Path $tmp 'Untamed-Realms-Windows.zip'
    Invoke-Download "$ReleaseBase/Untamed-Realms-Windows.zip" $zip
    Expand-Archive -Path $zip -DestinationPath $tmp -Force
    $src = Join-Path $tmp 'Untamed-Realms'
    if (-not (Test-Path (Join-Path $src 'scripts\common.ps1'))) { throw "The downloaded update looks incomplete; nothing was changed." }
    foreach ($item in Get-ChildItem $src) {
        $dest = Join-Path $Root $item.Name
        if ($item.PSIsContainer) {
            if (Test-Path $dest) { Remove-Item $dest -Recurse -Force }
            Copy-Item $item.FullName $dest -Recurse -Force
        } elseif (-not (Test-Path $dest) -or (Get-FileHash $item.FullName).Hash -ne (Get-FileHash $dest).Hash) {
            # Unchanged files (like the .bat that is running right now) are left alone.
            Copy-Item $item.FullName $dest -Force
        }
    }
    Remove-Item $tmp -Recurse -Force -ErrorAction SilentlyContinue
    Write-Done "  Updated to the latest build."
    return $true
}

# Runs the calling script again (after Update-Bundle replaced it) and exits with its result.
function Restart-Script([string]$scriptPath) {
    $env:UR_NO_UPDATE = '1'
    $shell = (Get-Process -Id $PID).Path
    & $shell -NoProfile -ExecutionPolicy Bypass -File $scriptPath
    exit $LASTEXITCODE
}
