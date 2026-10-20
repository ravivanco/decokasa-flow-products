param([switch]$Tipos)
$ErrorActionPreference='Continue'
$repoLocal=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\..\..'))
Set-Location (Join-Path $repoLocal 'frontend')
$env:GOOGLE_OAUTH_HABILITADA='false'
$env:GOOGLE_DRIVE_HABILITADA='false'
$env:GOOGLE_GMAIL_HABILITADA='false'
$env:COPIA_DRIVE_AUTOMATICA='false'
$env:CORREO_AUTOMATICO='false'
$env:VITE_ACCESO_LOCAL='false'
if($Tipos) {$argumentos=@('run','lint');$nombre='tipos'}
else {$argumentos=@('run','test','--','src/components/pedidos/Fase4Ramas.test.tsx','src/components/pedidos/Fase4Pantallas.test.tsx','--maxWorkers=2');$nombre='afectadas'}
$marca=Get-Date -Format 'yyyyMMdd-HHmmss'
& npm.cmd @argumentos 2>&1 | Tee-Object -FilePath (Join-Path $PSScriptRoot ($marca+'-'+$nombre+'.log'))
$codigo=$LASTEXITCODE
@{comando=('npm.cmd '+($argumentos -join ' '));salida=$codigo;fecha=$marca;googleReal=$false;backendEjecutado=$false;coberturaEjecutada=$false} | ConvertTo-Json | Set-Content -Encoding UTF8 (Join-Path $PSScriptRoot ($marca+'-'+$nombre+'.json'))
exit $codigo
