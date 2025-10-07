param(
    [Parameter(Mandatory=$true)][string]$GithubUser,
    [string]$RepoName = "WodifyPlus",
    [string]$DefaultBranch = "main"
)

$ErrorActionPreference = "Stop"

# Asegurar que estamos en la raíz del repo
if (-not (Test-Path ".git")) {
    Write-Error "No se encontró .git en el directorio actual. Ejecuta desde la raíz del proyecto o corre scripts/init_repo.ps1 primero."
    exit 1
}

# Asegura al menos un commit
$hasCommit = $false
try {
    git rev-parse --verify HEAD *> $null
    if ($LASTEXITCODE -eq 0) { $hasCommit = $true }
} catch {
    $hasCommit = $false
}

if (-not $hasCommit) {
    git add -A
    try { git commit -m "chore: initial commit" | Out-Null } catch {}
}

# Renombra/crea rama principal
try { git branch -M $DefaultBranch } catch { git switch -c $DefaultBranch | Out-Null }

# Configurar remoto
$remoteUrl = "https://github.com/$GithubUser/$RepoName.git"
$existing = $null
try { $existing = git remote get-url origin 2>$null } catch {}
if ($existing) {
    git remote set-url origin $remoteUrl
} else {
    git remote add origin $remoteUrl
}

git remote -v

# Push
git push -u origin $DefaultBranch
Write-Host "Subida completada a $remoteUrl ($DefaultBranch)" -ForegroundColor Green
