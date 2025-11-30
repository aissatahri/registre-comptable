<#
package-jpackage.ps1
Script PowerShell pour builder une image runtime et générer un installeur Windows (.msi ou .exe)
Utilisation : éditer les variables $JDK_HOME et $JAVA_FX_JMODS si besoin, puis lancer :

    .\package-jpackage.ps1

Ce script :
 - exécute `mvnw.cmd -DskipTests package`
 - détecte le jar produit dans `target/`
 - prépare un dossier d'entrée avec le jar et les dépendances (si présentes)
 - exécute `jlink` pour créer une runtime-image minimale
 - exécute `jpackage` pour créer l'installeur Windows

Remarque : `jlink` requiert les jmods (JDK/jmods et JavaFX jmods). Pour JavaFX, fournissez le dossier jmods de votre distribution JavaFX (souvent `javafx-jmods-<version>/`).
#>

param(
    [string]$JDK_HOME = "C:\Program Files\Zulu\zulu-22",          # Chemin vers le JDK qui contient jlink/jpackage
    [string]$JAVA_FX_JMODS = "C:\javafx-jmods-21.0.2",            # Chemin vers les jmods JavaFX (jmods), ex: javafx-jmods-21.0.2\jmods
    [switch]$DOWNLOAD_JAVAFX = $false,                              # Si true: télécharge automatiquement les JavaFX jmods depuis OpenJFX releases
    [string]$JAVAFX_VERSION = "21.0.2",                            # Version JavaFX à télécharger si demandé
    [string]$JAVAFX_ARCH = "x64",                                  # Architecture pour le binaire (ex: x64)
    [string]$JAVAFX_DOWNLOAD_DIR = "$env:USERPROFILE\Downloads",
    [string]$PROJECT_DIR = (Get-Location).Path,
    [string]$APP_NAME = "RegistreComptable",
    [string]$APP_VERSION = "1.0.0",
    [string]$MAIN_CLASS = "com.app.registre.Main",
    [string]$ICON_PATH = "icon.ico",                              # Optionnel : chemin vers .ico
    [string]$OUTPUT_DIR = "$PWD\dist",
    [string]$RUNTIME_IMAGE = "$PWD\runtime-image",
    [string]$INPUT_DIR = "$PWD\package-input",
    [string]$PACKAGE_TYPE = "exe",                                  # 'exe' (default) or 'msi'
    [switch]$DRY_RUN = $false,                                        # Si true: n'exécute pas jlink/jpackage, affiche seulement les commandes
    [switch]$SET_SHORTCUT_DB = $false,                                # Si true: ajoute --java-options pour définir -Ddb.url dans le launcher
    [string]$SHORTCUT_DB_URL = "jdbc:sqlite:%LOCALAPPDATA%\\RegistreComptable\\registre.db", # valeur placée dans --java-options si demandé
    [switch]$CREATE_LOCALAPPDATA_DIR = $false,                        # Si true: crée %LOCALAPPDATA%\RegistreComptable après packaging
    [switch]$ADD_SHORTCUT = $true                                     # Si false: n'ajoute pas d'options de raccourci
    ,[switch]$SKIP_MAVEN_BUILD = $false                               # Si true: n'exécute pas 'mvnw package' (utile si vous avez déjà construit le jar)
)

Write-Host "project dir: $PROJECT_DIR"
Write-Host "JDK_HOME: $JDK_HOME"
Write-Host "JAVA_FX_JMODS: $JAVA_FX_JMODS"

# Helpers
function Fail([string]$msg) {
    Write-Error $msg
    exit 1
}

# Verify jlink/jpackage exist
$JLINK = Join-Path $JDK_HOME "bin\jlink.exe"
$JPACKAGE = Join-Path $JDK_HOME "bin\jpackage.exe"
if (-not (Test-Path $JLINK)) { Fail "jlink not found at $JLINK. Assurez-vous que JDK_HOME est correct." }
if (-not (Test-Path $JPACKAGE)) { Fail "jpackage not found at $JPACKAGE. Assurez-vous que JDK_HOME est correct (JDK 14+)." }

# 1) Build project
Write-Host "==> Packaging Maven project..."
if ($SKIP_MAVEN_BUILD) {
    Write-Host "SKIP_MAVEN_BUILD: skipping maven package step (assuming jar already present in target)."
} else {
    if (-not $DRY_RUN) {
        Push-Location $PROJECT_DIR
        & .\mvnw.cmd "-Dmaven.test.skip=true" "package" # ou -DskipTests
        if ($LASTEXITCODE -ne 0) { Fail "Maven package failed." }
    } else {
        Write-Host "DRY_RUN: skipping maven package step (no build performed)."
    }
}

# 2) Find main jar produced in target
$targetDir = Join-Path $PROJECT_DIR "target"
$mainJar = Get-ChildItem -Path $targetDir -Filter "*.jar" | Where-Object { $_.Name -notlike "*-sources.jar" -and $_.Name -notlike "*-javadoc.jar" } | Select-Object -First 1

# Auto-download JavaFX jmods if requested or if provided path doesn't exist
try {
    $shouldDownload = $false
    if ($DOWNLOAD_JAVAFX) { $shouldDownload = $true }
    if (-not (Test-Path $JAVA_FX_JMODS)) { $shouldDownload = $true }

    if ($shouldDownload) {
        $version = $JAVAFX_VERSION
        $arch = $JAVAFX_ARCH
        $zipName = "openjfx-$version_windows-$arch_bin-jmods.zip"
        $zipPath = Join-Path $JAVAFX_DOWNLOAD_DIR $zipName
        $destPath = "C:\javafx-jmods-$version"

        if (Test-Path $destPath) {
            Write-Host "JavaFX jmods already extracted at $destPath"
            $JAVA_FX_JMODS = $destPath
        } else {
            $url = "https://github.com/openjdk/jfx/releases/download/$version/$zipName"
            Write-Host "Attempting to download JavaFX jmods from: $url"
            Write-Host "Saving to: $zipPath"
            try {
                Invoke-WebRequest -Uri $url -OutFile $zipPath -UseBasicParsing -ErrorAction Stop
            } catch {
                Fail "Failed to download JavaFX jmods from $url. $_"
            }
            Write-Host "Downloaded. Extracting to $destPath"
            try {
                if (-not (Test-Path $destPath)) { New-Item -ItemType Directory -Path $destPath | Out-Null }
                Expand-Archive -Path $zipPath -DestinationPath $destPath -Force
                $JAVA_FX_JMODS = $destPath
                Write-Host "Extraction complete. JavaFX jmods are in: $JAVA_FX_JMODS"
            } catch {
                Fail "Failed to extract JavaFX jmods: $_"
            }
        }
    } else {
        Write-Host "JavaFX jmods present at: $JAVA_FX_JMODS"
    }
} catch {
    Write-Host "Warning: error during JavaFX download/extract: $_"
}
if (-not $mainJar) { Fail "Impossible de trouver le jar principal dans target/. Assurez-vous que le projet a produit un jar." }
Write-Host "Main jar found: $($mainJar.Name)"

# 3) Prepare input dir for jpackage
if (Test-Path $INPUT_DIR) { Remove-Item -Recurse -Force $INPUT_DIR }
New-Item -ItemType Directory -Path $INPUT_DIR | Out-Null
Copy-Item -Path (Join-Path $targetDir $mainJar.Name) -Destination $INPUT_DIR

# Copy dependency jars if folder target/lib exists
$libDir = Join-Path $targetDir "lib"
if (Test-Path $libDir) {
    Copy-Item -Path (Join-Path $libDir "*") -Destination $INPUT_DIR -Recurse
}

# 4) Create runtime image with jlink
# jlink needs jmods from JDK ($JDK_HOME\jmods) and JavaFX jmods
$jmodsJdk = Join-Path $JDK_HOME "jmods"
if (-not (Test-Path $jmodsJdk)) { Fail "jmods folder not found in JDK (expected $jmodsJdk)." }
if (-not (Test-Path $JAVA_FX_JMODS)) { Fail "JavaFX jmods folder not found at $JAVA_FX_JMODS." }

# Modules to include
$modules = @(
    'java.base', 'java.logging', 'java.sql', 'java.desktop',
    'javafx.controls', 'javafx.fxml'
)
$moduleList = $modules -join ','

if (Test-Path $RUNTIME_IMAGE) { Remove-Item -Recurse -Force $RUNTIME_IMAGE }

$modulePath = "$jmodsJdk;$JAVA_FX_JMODS"
Write-Host "==> Running jlink to create runtime image (this may take a while)..."
$jlinkCmd = @(
    $JLINK,
    '--module-path', $modulePath,
    '--add-modules', $moduleList,
    '--output', $RUNTIME_IMAGE,
    '--strip-debug', '--compress', '2', '--no-header-files', '--no-man-pages'
)
Write-Host "jlink command: $($jlinkCmd -join ' ' )"
if (-not $DRY_RUN) {
    $exe = $jlinkCmd[0]
    $args = $jlinkCmd[1..($jlinkCmd.Length - 1)]
    & $exe $args
    if ($LASTEXITCODE -ne 0) { Fail "jlink failed." }
} else {
    Write-Host "DRY_RUN: skipping jlink execution. Runtime image not created." 
}

# 5) Run jpackage
# Ensure output dir exists
if (-not (Test-Path $OUTPUT_DIR)) { New-Item -ItemType Directory -Path $OUTPUT_DIR | Out-Null }

$mainJarName = $mainJar.Name
Write-Host "==> Running jpackage..."
 $cmd = @(
    $JPACKAGE,
    '--name', $APP_NAME,
    '--app-version', $APP_VERSION,
    '--input', $INPUT_DIR,
    '--main-jar', $mainJarName,
    '--main-class', $MAIN_CLASS,
    '--runtime-image', $RUNTIME_IMAGE,
    '--dest', $OUTPUT_DIR,
    '--type', $PACKAGE_TYPE
)

if (Test-Path $ICON_PATH) { $cmd += @('--icon', $ICON_PATH) }
if ($PACKAGE_TYPE -ne 'app-image') {
    # Windows-specific options only for installer types
    # Allow user to choose install dir
    $cmd += @('--win-dir-chooser')

    # Add shortcut/menu options if requested
    if ($ADD_SHORTCUT) {
        $cmd += @('--win-menu')
        if ($SET_SHORTCUT_DB) {
            # Add java options so that the created launcher includes -Ddb.url
            $javaOpt = "-Ddb.url=$SHORTCUT_DB_URL"
            $cmd += @('--java-options', $javaOpt)
        }
        # create a shortcut in Start Menu
        $cmd += @('--win-shortcut')
    }
} else {
    # For app-image, --java-options and windows-specific flags are not applicable
    Write-Host "PACKAGE_TYPE is 'app-image' — skipping Windows installer options (shortcuts, dir-chooser)."
}

Write-Host "jpackage command: $($cmd -join ' ' )"
if (-not $DRY_RUN) {
    $exe2 = $cmd[0]
    $args2 = $cmd[1..($cmd.Length - 1)]
    & $exe2 $args2
    if ($LASTEXITCODE -ne 0) { Fail "jpackage failed." }
} else {
    Write-Host "DRY_RUN: skipping jpackage execution. No installer was created." 
}

Write-Host "==> Packaging completed. Installer generated in: $OUTPUT_DIR"
Pop-Location

Write-Host "Done."
# Post-install helpers
if (-not $DRY_RUN -and $CREATE_LOCALAPPDATA_DIR) {
    $appDataDir = Join-Path $env:LOCALAPPDATA "RegistreComptable"
    if (-not (Test-Path $appDataDir)) { New-Item -ItemType Directory -Path $appDataDir | Out-Null ; Write-Host "Created $appDataDir" }
}
