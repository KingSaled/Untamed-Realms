# Starts over for playtesting: moves the server's world (and every character in it) aside so the next
# Start-Server.bat generates a brand-new world with a new random seed. The 3 newest old worlds are kept.
. "$PSScriptRoot\common.ps1"

Write-Host ""
Write-Host "  UNTAMED REALMS - start a fresh $(if ($env:UR_TEST_WORLD -eq '1') { 'TEST world' } else { 'world' })" -ForegroundColor Yellow

$server = Join-Path $Root 'server'
# Reset-Test-World.bat sets UR_TEST_WORLD=1 to reset the test world instead.
$worldName = if ($env:UR_TEST_WORLD -eq '1') { 'test-world' } else { 'world' }
$world = Join-Path $server $worldName
$starter = if ($env:UR_TEST_WORLD -eq '1') { 'Start-Test-World.bat' } else { 'Start-Server.bat' }
if (-not (Test-Path $world)) {
    Write-Done "There is no world yet. $starter will create a fresh one."
    return
}

# A running server keeps world\session.lock open, so we cannot take it exclusively.
$lockFile = Join-Path $world 'session.lock'
if (Test-Path $lockFile) {
    try { [IO.File]::Open($lockFile, 'Open', 'ReadWrite', 'None').Close() }
    catch { throw "The server is still running. Type  stop  in the server window, wait for it to close, then run this again." }
}

Write-Host ""
Write-Host "This moves the current world - including your character, skills, quests and the village" -ForegroundColor Yellow
Write-Host "you started in - to server\old-worlds. The next $starter creates a brand-new world." -ForegroundColor Yellow
if ($env:UR_ASSUME_YES -ne '1') {
    $answer = Read-Host "Type Y and press Enter to continue"
    if ($answer -notmatch '^\s*[Yy]') { Write-Host "Nothing was changed."; return }
}

$archive = Join-Path $server 'old-worlds'
New-Item -ItemType Directory -Force -Path $archive | Out-Null
$name = "$worldName-" + (Get-Date -Format 'yyyy-MM-dd_HH-mm-ss')
Move-Item -Path $world -Destination (Join-Path $archive $name)
Get-ChildItem $archive -Directory -Filter "$worldName-2*" | Sort-Object Name -Descending | Select-Object -Skip 3 | Remove-Item -Recurse -Force

Write-Host ""
Write-Done "Done! Run $starter to generate a fresh world (new seed, new character)."
Write-Host "  The old world was saved as server\old-worlds\$name (the 3 newest are kept)." -ForegroundColor DarkGray
