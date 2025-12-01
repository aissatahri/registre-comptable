# Script de test du système de mise à jour
param(
    [string]$FromVersion = "1.4.6",
    [string]$ToVersion = "1.4.7"
)

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "TEST SYSTÈME DE MISE À JOUR" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

Write-Host "Configuration:" -ForegroundColor Yellow
Write-Host "  Version source: $FromVersion" -ForegroundColor White
Write-Host "  Version cible: $ToVersion (sur GitHub)" -ForegroundColor White

# Tuer les processus Java existants
Write-Host "`n[1/5] Arrêt des processus Java..." -ForegroundColor Yellow
Get-Process -Name java -ErrorAction SilentlyContinue | Stop-Process -Force
Start-Sleep -Seconds 1

# Vérifier que le JAR source existe
$jarPath = "release-v$FromVersion\registre-comptable-$FromVersion.jar"
if (-not (Test-Path $jarPath)) {
    Write-Host "❌ ERREUR: $jarPath n'existe pas!" -ForegroundColor Red
    exit 1
}

Write-Host "✅ JAR trouvé: $jarPath" -ForegroundColor Green
$jarSize = (Get-Item $jarPath).Length
Write-Host "   Taille: $([math]::Round($jarSize/1MB, 2)) MB" -ForegroundColor Gray

# Vérifier la version sur GitHub
Write-Host "`n[2/5] Vérification de la version sur GitHub..." -ForegroundColor Yellow
try {
    $release = curl -s "https://api.github.com/repos/aissatahri/registre-comptable/releases/latest" | ConvertFrom-Json
    Write-Host "✅ Dernière version GitHub: $($release.tag_name)" -ForegroundColor Green
    
    if ($release.tag_name -ne "v$ToVersion") {
        Write-Host "⚠️  WARNING: La version attendue est v$ToVersion mais GitHub a $($release.tag_name)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ Impossible de vérifier GitHub: $($_.Exception.Message)" -ForegroundColor Red
}

# Créer un fichier de log
$logFile = "test-update-$FromVersion-to-$ToVersion.log"
Write-Host "`n[3/5] Fichier de log: $logFile" -ForegroundColor Yellow

Write-Host "`n[4/5] Lancement de l'application v$FromVersion..." -ForegroundColor Yellow
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray
Write-Host "INSTRUCTIONS:" -ForegroundColor Cyan
Write-Host "  1. Attendez que l'application s'ouvre" -ForegroundColor White
Write-Host "  2. Connexion: admin / admin" -ForegroundColor White
Write-Host "  3. Menu → Aide → À propos (vérifier version)" -ForegroundColor White
Write-Host "  4. Menu → Aide → Vérifier les mises à jour" -ForegroundColor White
Write-Host "  5. Observez les logs ci-dessous" -ForegroundColor White
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━`n" -ForegroundColor Gray

Write-Host "[5/5] LOGS EN DIRECT:" -ForegroundColor Yellow
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━`n" -ForegroundColor Gray

cd "release-v$FromVersion"
java -jar "registre-comptable-$FromVersion.jar" 2>&1 | Tee-Object -FilePath "../$logFile"
