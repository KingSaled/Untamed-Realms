# One-click Untamed Realms server: installs / updates everything, then starts it in this window.
# Type commands (e.g. "op YourName", "stop") directly into this window.
. "$PSScriptRoot\common.ps1"

Write-Host ""
Write-Host "  UNTAMED REALMS - dedicated server" -ForegroundColor Yellow
Write-Host "  Minecraft $MinecraftVersion + NeoForge $NeoForgeVersion"

if (Update-Bundle) { Restart-Script $PSCommandPath }

$server = Join-Path $Root 'server'
New-Item -ItemType Directory -Force -Path $server | Out-Null
$java = Get-Java

# 1. NeoForge server
$argsFile = "libraries\net\neoforged\neoforge\$NeoForgeVersion\win_args.txt"
if (-not (Test-Path (Join-Path $server $argsFile))) {
    $installer = Get-NeoForgeInstaller
    Write-Step "Installing the NeoForge server (one time)"
    Push-Location $server
    try { $code = Invoke-Native $java @('-jar', $installer, '--installServer') } finally { Pop-Location }
    if (-not (Test-Path (Join-Path $server $argsFile))) { throw "NeoForge server install failed (code $code)." }
    Remove-Item (Join-Path $server 'run.bat'), (Join-Path $server 'run.sh') -ErrorAction SilentlyContinue
}

# 2. Mods (server side only) + config
Write-Step "Checking mods"
Sync-Mods (Join-Path $server 'mods') 'server'
Copy-PackConfig $server

# 3. First-run files
if (-not (Test-Path (Join-Path $server 'server.properties'))) {
    Copy-Item (Join-Path $PSScriptRoot 'server.properties') (Join-Path $server 'server.properties')
}
Write-Host ""
Write-Host "By running the server you agree to the Minecraft EULA: https://aka.ms/MinecraftEULA" -ForegroundColor DarkGray
Set-Content -Path (Join-Path $server 'eula.txt') -Value 'eula=true'

$ram = Get-RamGB
$heap = if ($ram -ge 32) { 8 } elseif ($ram -ge 24) { 6 } elseif ($ram -ge 16) { 5 } else { 4 }
if ($ram -lt 12) { Write-Warning "This PC has $ram GB RAM. Running the server AND the game here may be slow; close other programs." }
$jvm = @(
    "-Xms2G", "-Xmx${heap}G",
    "-XX:+UseG1GC", "-XX:+ParallelRefProcEnabled", "-XX:MaxGCPauseMillis=200", "-XX:+UnlockExperimentalVMOptions",
    "-XX:+DisableExplicitGC", "-XX:G1NewSizePercent=30", "-XX:G1MaxNewSizePercent=40", "-XX:G1HeapRegionSize=8M",
    "-XX:G1ReservePercent=20", "-XX:G1HeapWastePercent=5", "-XX:G1MixedGCCountTarget=4",
    "-XX:InitiatingHeapOccupancyPercent=15", "-XX:G1MixedGCLiveThresholdPercent=90", "-XX:G1RSetUpdatingPauseTimePercent=5",
    "-XX:SurvivorRatio=32", "-XX:+PerfDisableSharedMem", "-XX:MaxTenuringThreshold=1", "-Dfile.encoding=UTF-8"
)
Set-Content -Path (Join-Path $server 'user_jvm_args.txt') -Value $jvm

# 4. Run
Write-Step "Starting the server with $heap GB memory"
Write-Host "  First start generates the world and takes a few minutes. Wait for: Done (..s)! For help, type ""help""" -ForegroundColor DarkGray
Write-Host "  Then join from the game: Multiplayer -> Direct Connection -> localhost" -ForegroundColor DarkGray
Write-Host "  Make yourself admin by typing:  op YourMinecraftName      Stop the server with:  stop" -ForegroundColor DarkGray
Write-Host "  If Windows Firewall asks about Java, allow it (Private networks)." -ForegroundColor DarkGray
Write-Host ""
Push-Location $server
try {
    $ErrorActionPreference = 'Continue'
    if ($env:UR_SELF_TEST -eq '1') {
        # Automated test mode (used by CI): start, wait for "Done", then stop cleanly.
        $psi = New-Object System.Diagnostics.ProcessStartInfo
        $psi.FileName = $java
        $psi.Arguments = "@user_jvm_args.txt @$argsFile --nogui"
        $psi.WorkingDirectory = $server
        $psi.UseShellExecute = $false
        $psi.RedirectStandardInput = $true
        $proc = [System.Diagnostics.Process]::Start($psi)
        $log = Join-Path $server 'logs\latest.log'
        $deadline = (Get-Date).AddMinutes(25)
        while (-not $proc.HasExited -and (Get-Date) -lt $deadline) {
            Start-Sleep -Seconds 5
            if ((Test-Path $log) -and (Select-String -Path $log -Pattern 'Done \(' -Quiet)) {
                # Windows PowerShell prefixes the first line written to stdin with a byte-order mark, so send
                # a blank line first (the server ignores it as an unknown command), then stop.
                $proc.StandardInput.WriteLine('')
                $proc.StandardInput.WriteLine('stop')
                break
            }
        }
        if (-not $proc.WaitForExit(180000)) { $proc.Kill(); throw "Server did not stop in time" }
        if (-not (Select-String -Path $log -Pattern 'Done \(' -Quiet)) { throw "Server did not finish starting" }
    } else {
        & $java '@user_jvm_args.txt' "@$argsFile" '--nogui'
    }
} finally { Pop-Location }
Write-Host ""
Write-Done "The server has stopped."
