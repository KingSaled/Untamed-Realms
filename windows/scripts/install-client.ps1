# Installs Untamed Realms into the official Minecraft Launcher as its own installation ("Untamed Realms").
# Safe to run again at any time to update. Your normal Minecraft worlds and settings are not touched:
# the modpack lives in %APPDATA%\.minecraft\untamed-realms.
. "$PSScriptRoot\common.ps1"

Write-Host ""
Write-Host "  UNTAMED REALMS - modpack installer for the Minecraft Launcher" -ForegroundColor Yellow
Write-Host "  Minecraft $MinecraftVersion + NeoForge $NeoForgeVersion"

$mc = Join-Path $env:APPDATA '.minecraft'
if (-not (Test-Path $mc)) {
    throw "Could not find $mc. Open the Minecraft Launcher once and log in, then run this installer again."
}

# The launcher rewrites its profile file when it closes, so it must not be running while we edit it.
while (Get-Process -Name 'MinecraftLauncher', 'Minecraft' -ErrorAction SilentlyContinue) {
    Write-Host ""
    Write-Host "Please CLOSE the Minecraft Launcher (and Minecraft), then press Enter..." -ForegroundColor Yellow
    [void](Read-Host)
}

$java = Get-Java

# 1. NeoForge for the client (adds the "neoforge-<version>" version to the launcher)
$versionId = "neoforge-$NeoForgeVersion"
if (Test-Path "$mc\versions\$versionId\$versionId.json") {
    Write-Step "NeoForge $NeoForgeVersion is already installed"
} else {
    $profiles = Join-Path $mc 'launcher_profiles.json'
    if (-not (Test-Path $profiles)) { Write-Utf8NoBom $profiles '{"profiles":{}}' }   # the installer needs this file
    $installer = Get-NeoForgeInstaller
    Write-Step "Installing NeoForge $NeoForgeVersion into the launcher"
    $code = Invoke-Native $java @('-jar', $installer, '--installClient', $mc)
    if (-not (Test-Path "$mc\versions\$versionId\$versionId.json")) {
        Write-Host ""
        Write-Host "The automatic install did not finish (code $code). The NeoForge installer window will open now:" -ForegroundColor Yellow
        Write-Host "  -> keep 'Install client' selected and click OK, then close it when it says it is done." -ForegroundColor Yellow
        Start-Process -FilePath $java -ArgumentList @('-jar', "`"$installer`"") -Wait
        if (-not (Test-Path "$mc\versions\$versionId\$versionId.json")) { throw "NeoForge was not installed. Please try again." }
    }
    Write-Done "  NeoForge installed"
}

# 2. Mods + config into a separate game folder
$game = Join-Path $mc 'untamed-realms'
Write-Step "Downloading the modpack (first time: a few minutes)"
Sync-Mods (Join-Path $game 'mods') 'client'
Copy-PackConfig $game

# 3. A launcher installation pointing at that folder, with sensible memory
$ram = Get-RamGB
$heap = if ($ram -ge 24) { 8 } elseif ($ram -ge 16) { 6 } else { 4 }
$javaArgs = "-Xmx${heap}G -Xms2G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20"
$now = (Get-Date).ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ss.fffZ')
$updated = 0
foreach ($name in 'launcher_profiles.json', 'launcher_profiles_microsoft_store.json') {
    $path = Join-Path $mc $name
    if (-not (Test-Path $path)) { continue }
    $json = Get-Content $path -Raw | ConvertFrom-Json
    if (-not $json.profiles) { $json | Add-Member -NotePropertyName profiles -NotePropertyValue ([pscustomobject]@{}) -Force }
    $profile = [pscustomobject]@{
        name          = 'Untamed Realms'
        type          = 'custom'
        created       = $now
        lastUsed      = $now
        icon          = 'Bookshelf'
        lastVersionId = $versionId
        gameDir       = $game
        javaArgs      = $javaArgs
    }
    $json.profiles | Add-Member -NotePropertyName 'untamed-realms' -NotePropertyValue $profile -Force
    Write-Utf8NoBom $path ($json | ConvertTo-Json -Depth 32)
    $updated++
}
if ($updated -eq 0) { throw "No launcher profile file found in $mc - open the Minecraft Launcher once, then run this again." }

Write-Host ""
Write-Done "=================================================================="
Write-Done " Done! Untamed Realms is installed ($heap GB memory)."
Write-Done ""
Write-Done " 1. Open the Minecraft Launcher"
Write-Done " 2. Next to the green PLAY button pick 'Untamed Realms'"
Write-Done " 3. Press PLAY (the first start takes a couple of minutes)"
Write-Done " 4. Multiplayer -> Direct Connection -> localhost   (if the server is on this PC)"
Write-Done "=================================================================="
