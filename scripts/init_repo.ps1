param(
    [string]$RepoName = "WodifyPlus",
    [string]$GitUser = "",
    [string]$GitEmail = ""
)

$ErrorActionPreference = "Stop"

# Moverse a la raíz del proyecto (carpeta padre de scripts)
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $repoRoot

if ($GitUser -ne "") { git config user.name $GitUser }
if ($GitEmail -ne "") { git config user.email $GitEmail }

if (-not (Test-Path .git)) {
    git init
}

# Asegurar existencia de .gitignore en raíz
if (-not (Test-Path ".gitignore")) {
    New-Item -ItemType File -Path ".gitignore" | Out-Null
}

# Asegurar .env y caché de Python ignorados
$ignoreLines = @(
    "app/src/main/python/.env",
    "app/src/main/python/__pycache__/",
    "*.pyc"
)
foreach ($line in $ignoreLines) {
    $escaped = [regex]::Escape($line)
    $pattern = "^$escaped$"
    $exists = Select-String -Path ".gitignore" -Pattern $pattern -Quiet -ErrorAction SilentlyContinue
    if (-not $exists) {
        Add-Content -Path ".gitignore" -Value $line
    }
}

# Añadir todo y commit inicial si procede
git add .
try {
    git commit -m "chore: initial docs and project setup"
} catch {}

Write-Host "Repositorio local inicializado en $repoRoot" -ForegroundColor Green
Write-Host "Ahora crea el repo en GitHub y ejecuta:" -ForegroundColor Yellow
Write-Host "  git remote add origin https://github.com/<tu-usuario>/$RepoName.git"
Write-Host "  git branch -M main"
Write-Host "  git push -u origin main"
