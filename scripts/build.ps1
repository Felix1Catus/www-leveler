# Build the WWW Leveler plugin (.nbm) against a fresh UGS source checkout.
# Requires: JDK 25 on PATH (or JAVA_HOME), git.
#
#   powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1
#
$ErrorActionPreference = "Stop"

$tag   = if ($env:UGS_TAG) { $env:UGS_TAG } else { "v2.1.27" }
$here  = Split-Path -Parent $PSScriptRoot
$build = Join-Path $here ".ugs-build"

# --- JDK 25 check ---
$javac = Get-Command javac -ErrorAction SilentlyContinue
if (-not $javac) {
    throw "javac not found. Install JDK 25 and put it on PATH (or set JAVA_HOME)."
}
$ver = (& javac -version 2>&1)
if ($ver -notmatch " 25(\.|$| )") {
    Write-Warning "javac is not version 25 ($ver). UGS 2.1.x targets Java 25; the build may fail."
}

# --- fresh UGS checkout ---
if (-not (Test-Path (Join-Path $build ".git"))) {
    Write-Host ">> cloning UGS $tag ..."
    if (Test-Path $build) { Remove-Item -Recurse -Force $build }
    git clone --depth 1 --branch $tag https://github.com/winder/Universal-G-Code-Sender.git $build
    if ($LASTEXITCODE -ne 0) { throw "git clone failed" }
} else {
    Write-Host ">> reusing existing checkout at $build"
}

# --- inject this module ---
Write-Host ">> injecting WWWLeveler module ..."
$mod = Join-Path $build "ugs-platform\WWWLeveler"
if (Test-Path $mod) { Remove-Item -Recurse -Force $mod }
New-Item -ItemType Directory -Force -Path $mod | Out-Null
Copy-Item -Recurse -Force (Join-Path $here "plugin\*") $mod

# --- register module in the platform parent pom (idempotent) ---
$pom = Join-Path $build "ugs-platform\pom.xml"
$content = Get-Content -Raw $pom
if ($content -notmatch "<module>WWWLeveler</module>") {
    Write-Host ">> registering module in $pom ..."
    $content = $content -replace "(\s*)</modules>", "`$1  <module>WWWLeveler</module>`$1</modules>"
    Set-Content -Path $pom -Value $content -Encoding utf8
}

# --- build ---
Write-Host ">> building ..."
Push-Location $build
try {
    & .\mvnw.cmd install -pl ugs-platform/WWWLeveler -am -DskipTests -B
    if ($LASTEXITCODE -ne 0) { throw "Maven build failed (exit $LASTEXITCODE)" }
} finally {
    Pop-Location
}

# --- collect artifact ---
$dist = Join-Path $here "dist"
New-Item -ItemType Directory -Force -Path $dist | Out-Null
$nbm = Get-ChildItem (Join-Path $mod "target") -Filter "*.nbm" | Select-Object -First 1
Copy-Item -Force $nbm.FullName $dist
Write-Host ""
Write-Host "BUILD OK -> $(Join-Path $dist $nbm.Name)"
