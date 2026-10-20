$ErrorActionPreference='Continue'
Set-Location $PSScriptRoot
$env:VITE_ACCESO_LOCAL='false'
$env:GOOGLE_OAUTH_HABILITADA='false'
$env:GOOGLE_DRIVE_HABILITADA='false'
$env:GOOGLE_GMAIL_HABILITADA='false'
$env:COPIA_DRIVE_AUTOMATICA='false'
$env:CORREO_AUTOMATICO='false'
$marca=Get-Date -Format 'yyyyMMdd-HHmmss'
$env:F4_OUTPUT_DIR='./browser-artifacts-'+$marca
$env:F4_REPORT='./'+$marca+'-playwright-resultados.json'
& node 'C:/Users/Richard/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/cli.js' test --config=playwright.config.mjs 2>&1 | Tee-Object -FilePath ($marca+'-playwright.log')
$codigo=$LASTEXITCODE
@{comando='node playwright/cli.js test --config=playwright.config.mjs';salida=$codigo;googleReal=$false;api='simulada';recorridoA1Fin=$false} | ConvertTo-Json | Set-Content -Encoding UTF8 ($marca+'-playwright.json')
exit $codigo
