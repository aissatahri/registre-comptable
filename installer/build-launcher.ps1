# PowerShell helper to run Launch4j and produce launcher.exe
# Usage: open PowerShell in project root and run: .\installer\build-launcher.ps1

# Try to find launch4j executable
$pf = $Env:ProgramFiles
$pf86 = ${env:ProgramFiles(x86)}
$possible = @(
    "$pf\Launch4j\launch4j.exe",
    "$pf86\Launch4j\launch4j.exe",
    "$pf\Launch4j\bin\launch4j.exe",
    "$pf86\Launch4j\bin\launch4j.exe"
)
$found = $null
foreach ($p in $possible) {
    if (Test-Path $p) { $found = $p; break }
}

if (-not $found) {
    Write-Host "Could not find launch4j.exe in common locations. Please install Launch4j or set LAUNCH4J env var."
    if ($Env:LAUNCH4J) { if (Test-Path $Env:LAUNCH4J) { $found = $Env:LAUNCH4J } }
}

if (-not $found) {
    Write-Error "launch4j.exe not found. Install Launch4j (https://launch4j.sourceforge.net/) and rerun this script."; exit 2
}

$cfg = Join-Path -Path (Get-Location) -ChildPath "installer\launch4j.xml"
if (-not (Test-Path $cfg)) { Write-Error "launch4j config not found at $cfg"; exit 3 }

Write-Host "Running Launch4j: $found with config $cfg"
& $found $cfg
if ($LASTEXITCODE -ne 0) { Write-Error "Launch4j failed with exit code $LASTEXITCODE"; exit $LASTEXITCODE }

Write-Host "Launcher built: installer\launcher-1.1.0.exe (or launcher-1.1.0.exe in current folder). Copy or include it in the installer before compiling." 
