$ErrorActionPreference = 'Stop'
$verificationRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $verificationRoot
$verificationEnv = Join-Path $verificationRoot '.verification/compose.env'
if (!(Test-Path -LiteralPath $verificationEnv)) {
    New-Item -ItemType Directory -Path (Join-Path $verificationRoot '.verification') -Force | Out-Null
    $verificationRng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $verificationValues = @()
    foreach ($verificationName in @('KR3_VERIFY_DB_PASSWORD', 'KR3_VERIFY_ADMIN_PASSWORD', 'KR3_VERIFY_NEW_PASSWORD', 'KR3_VERIFY_JWT_SECRET')) {
        $verificationBytes = New-Object byte[] 32
        $verificationRng.GetBytes($verificationBytes)
        $verificationValues += $verificationName + '=' + [Convert]::ToBase64String($verificationBytes)
    }
    $verificationRng.Dispose()
    [System.IO.File]::WriteAllLines($verificationEnv, $verificationValues, (New-Object System.Text.UTF8Encoding($false)))
}
docker compose --env-file $verificationEnv -p kr3-verify -f compose.verify.yaml config --quiet
if ($LASTEXITCODE -ne 0) { throw 'Compose configuration failed' }
docker compose --env-file $verificationEnv -p kr3-verify -f compose.verify.yaml up -d --build --wait --wait-timeout 180
if ($LASTEXITCODE -ne 0) { throw 'Compose startup failed' }
node scripts/verify-live.cjs
if ($LASTEXITCODE -ne 0) { throw 'Live acceptance checks failed' }
docker compose --env-file $verificationEnv -p kr3-verify -f compose.verify.yaml down
if ($LASTEXITCODE -ne 0) { throw 'Verification shutdown failed' }
docker compose --env-file $verificationEnv -p kr3-verify -f compose.verify.yaml up -d --wait --wait-timeout 180
if ($LASTEXITCODE -ne 0) { throw 'Verification restart failed' }
node scripts/verify-persistence.cjs
if ($LASTEXITCODE -ne 0) { throw 'Persistence check failed' }
