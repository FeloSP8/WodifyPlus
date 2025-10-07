param(
    [string]$CommitMsg = "docs: update project documentation"
)

$ErrorActionPreference = "Stop"

# Verifica remoto
$remotes = git remote
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($remotes)) {
    Write-Error "No hay remoto configurado. Ejecuta scripts/init_repo.ps1 primero."
    exit 1
}

# Garantiza que el .env no se sube
if (-not (Select-String -Path ".gitignore" -Pattern "app/src/main/python/.env" -Quiet)) {
    Add-Content -Path ".gitignore" -Value "app/src/main/python/.env"
}

# Añadir documentación y cambios
$docs = @(
    "README.md",
    "CHANGELOG.md",
    "ARCHITECTURE.md",
    "CONTRIBUTING.md",
    "WIDGET_DEBUG.md"
)

foreach ($f in $docs) {
    if (Test-Path $f) { git add $f }
}

# Añadir .gitignore por si cambió
git add .gitignore

# Commit y push
try {
    git commit -m $CommitMsg
} catch {}

git push -u origin HEAD
Write-Host "Documentación publicada." -ForegroundColor Green
