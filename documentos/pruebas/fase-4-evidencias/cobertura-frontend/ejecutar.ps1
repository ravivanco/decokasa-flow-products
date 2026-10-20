param([ValidateSet('baseline','dirigido','coverage','lint','build')][string]$Modo)
$ErrorActionPreference='Continue'
$repoLocal=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\..\..'))
Set-Location (Join-Path $repoLocal 'frontend')
$env:GOOGLE_OAUTH_HABILITADA='false'
$env:GOOGLE_DRIVE_HABILITADA='false'
$env:GOOGLE_GMAIL_HABILITADA='false'
$env:COPIA_DRIVE_AUTOMATICA='false'
$env:CORREO_AUTOMATICO='false'
$env:VITE_ACCESO_LOCAL='false'
if($Modo -in @('baseline','coverage')) {$argumentos=@('run','coverage','--','--maxWorkers=2','--coverage.include=src/**/*.{ts,tsx}','--coverage.reporter=text','--coverage.reporter=json','--coverage.reporter=json-summary')}
elseif($Modo -eq 'dirigido') {$argumentos=@('run','test','--','src/components/pedidos/Fase4Pantallas.test.tsx','src/components/pedidos/Fase4Ramas.test.tsx','src/components/pedidos/formato.test.ts','--maxWorkers=2')}
else {$argumentos=@('run',$Modo)}
$marca=Get-Date -Format 'yyyyMMdd-HHmmss'
& npm.cmd @argumentos 2>&1 | Tee-Object -FilePath (Join-Path $PSScriptRoot ($marca+'-'+$Modo+'.log'))
$codigo=$LASTEXITCODE
@{comando=('npm.cmd '+($argumentos -join ' '));salida=$codigo;fecha=$marca;googleReal=$false;backendEjecutado=$false} | ConvertTo-Json | Set-Content -Encoding UTF8 (Join-Path $PSScriptRoot ($marca+'-'+$Modo+'.json'))
if($Modo -in @('baseline','coverage')) {
 Copy-Item 'coverage/coverage-summary.json' (Join-Path $PSScriptRoot ($marca+'-'+$Modo+'-summary.json'))
 Copy-Item 'coverage/coverage-final.json' (Join-Path $PSScriptRoot ($marca+'-'+$Modo+'-contadores.json'))
}
exit $codigo
