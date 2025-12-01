# Script pour générer le manifest update4j.xml avec les vrais checksums
param(
    [Parameter(Mandatory=$true)]
    [string]$Version
)

$jarPath = "target\registre-comptable-$Version.jar"

if (-not (Test-Path $jarPath)) {
    Write-Host "❌ Fichier $jarPath introuvable !" -ForegroundColor Red
    Write-Host "Compilez d'abord avec: .\mvnw.cmd package -DskipTests" -ForegroundColor Yellow
    exit 1
}

# Calculer la taille
$fileInfo = Get-Item $jarPath
$size = $fileInfo.Length

# Calculer le checksum (MD5 en hexa)
$md5 = Get-FileHash -Path $jarPath -Algorithm MD5
$checksum = $md5.Hash.ToLower()

Write-Host "📦 Fichier: $jarPath" -ForegroundColor Cyan
Write-Host "📏 Taille: $size octets" -ForegroundColor Cyan
Write-Host "🔒 Checksum MD5: $checksum" -ForegroundColor Cyan

# Générer le XML SANS basePath - update4j téléchargera dans un temp dir
$xml = @"
<?xml version="1.0" encoding="UTF-8"?>
<!-- Manifest update4j généré automatiquement pour v$Version -->
<configuration base="https://github.com/aissatahri/registre-comptable/releases/download/v$Version/" version="$Version">
    <provider>
        <name>GitHub Releases</name>
    </provider>

    <!-- JAR principal de l'application -->
    <file path="registre-comptable-$Version.jar" size="$size" checksum="$checksum"/>

    <!-- Launcher: classe principale à exécuter après mise à jour -->
    <launcher class="com.app.registre.Main">
        <vmArg>-Xmx512m</vmArg>
    </launcher>
</configuration>
"@

# Sauvegarder dans resources
$outputPath = "src\main\resources\update4j.xml"
$xml | Out-File -FilePath $outputPath -Encoding UTF8

Write-Host "`n✅ Manifest généré: $outputPath" -ForegroundColor Green
Write-Host "`nContenu:" -ForegroundColor Yellow
Get-Content $outputPath | Write-Host -ForegroundColor White
