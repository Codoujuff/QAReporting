# Sauvegarde de QA Reporting : base MariaDB + pièces jointes, avec rotation.
#
# Usage manuel :      powershell -ExecutionPolicy Bypass -File scripts\sauvegarde.ps1
# Tous les soirs :    voir docs/GUIDE-INSTALLATION.md, « Sauvegarde automatique » (Planificateur de tâches).
#
# Paramètres par variables d'environnement (mêmes noms que la datasource) :
#   QA_DB_USER / QA_DB_PASSWORD / QA_DB_NAME   (root / vide / qa_reporting_j2ee par défaut)
#   QA_BACKUP_DIR     dossier de destination   (backups\ à la racine du projet par défaut)
#   QA_BACKUP_KEEP    nombre de sauvegardes gardées (14 par défaut)
#   QA_MYSQLDUMP      chemin de mysqldump      (C:\xampp\mysql\bin\mysqldump.exe par défaut)
#   QA_ATTACHMENTS    dossier des pièces jointes (celui de WildFly par défaut)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
function Get-Setting($name, $default) {
    $value = [Environment]::GetEnvironmentVariable($name)
    if ([string]::IsNullOrEmpty($value)) { return $default } else { return $value }
}

$dbUser      = Get-Setting "QA_DB_USER" "root"
$dbPassword  = Get-Setting "QA_DB_PASSWORD" ""
$dbName      = Get-Setting "QA_DB_NAME" "qa_reporting_j2ee"
$backupDir   = Get-Setting "QA_BACKUP_DIR" (Join-Path $projectRoot "backups")
$keep        = [int](Get-Setting "QA_BACKUP_KEEP" "14")
$mysqldump   = Get-Setting "QA_MYSQLDUMP" "C:\xampp\mysql\bin\mysqldump.exe"
$attachments = Get-Setting "QA_ATTACHMENTS" (Join-Path $projectRoot "target\server\standalone\data\qa-reporting-j2ee\attachments")

$stamp  = Get-Date -Format "yyyy-MM-dd_HHmm"
$target = Join-Path $backupDir "qa-reporting_$stamp"
New-Item -ItemType Directory -Force -Path $target | Out-Null
try {

# 1. Base de données (transaction cohérente, sans bloquer l'application)
$dumpFile = Join-Path $target "base.sql"
$dumpArgs = @("-u$dbUser", "--single-transaction", "--routines", "--default-character-set=utf8mb4", "--result-file=$dumpFile", $dbName)
if ($dbPassword -ne "") { $dumpArgs = @("-p$dbPassword") + $dumpArgs }
& $mysqldump @dumpArgs
if ($LASTEXITCODE -ne 0) { throw "mysqldump a échoué (code $LASTEXITCODE)" }

# 2. Pièces jointes (fichiers hors base)
if (Test-Path $attachments) {
    Copy-Item -Recurse -Force $attachments (Join-Path $target "attachments")
}

# 3. Archive, puis rotation : on garde les $keep plus récentes
$zip = "$target.zip"
Compress-Archive -Path "$target\*" -DestinationPath $zip -Force
} finally {
    # dossier de travail supprimé dans tous les cas, même si la sauvegarde a échoué
    if (Test-Path $target) { Remove-Item -Recurse -Force $target }
}
Get-ChildItem $backupDir -Filter "qa-reporting_*.zip" | Sort-Object LastWriteTime -Descending |
    Select-Object -Skip $keep | Remove-Item -Force

Write-Output "Sauvegarde créée : $zip"
