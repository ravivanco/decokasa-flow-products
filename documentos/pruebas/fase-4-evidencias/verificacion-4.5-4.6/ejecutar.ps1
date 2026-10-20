param([ValidateSet('lint','test','build','coverage','dirigido','check','jacoco')][string]$Modo)
$ErrorActionPreference='Continue'
$repoLocal=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\..\..'))
$env:GOOGLE_OAUTH_HABILITADA='false'
$env:GOOGLE_DRIVE_HABILITADA='false'
$env:GOOGLE_GMAIL_HABILITADA='false'
$env:COPIA_DRIVE_AUTOMATICA='false'
$env:CORREO_AUTOMATICO='false'
$env:SPRING_CONFIG_IMPORT=''
$env:SEMILLA_HABILITADA='false'
$env:DB_URL='jdbc:postgresql://127.0.0.1:1/sin-base-compartida'
$env:DB_USUARIO='prueba-ficticia'
$env:DB_CONTRASENA='prueba-ficticia'
$env:VITE_ACCESO_LOCAL='false'
$env:ALMACEN_LOCAL_DIR=Join-Path ([IO.Path]::GetTempPath()) ('decokasa-f4b-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $env:ALMACEN_LOCAL_DIR -Force | Out-Null
$marca=Get-Date -Format 'yyyyMMdd-HHmmss'
if($Modo -in @('lint','test','build','coverage')) {
 Set-Location (Join-Path $repoLocal 'frontend')
 $argumentos=@('run',$Modo)
 if($Modo -in @('test','coverage')) {$argumentos+=@('--','--maxWorkers=2')}
 if($Modo -eq 'coverage') {$argumentos+=@('--coverage.include=src/**/*.{ts,tsx}','--coverage.reporter=text','--coverage.reporter=json','--coverage.reporter=json-summary','--coverage.reporter=html')}
 & npm.cmd @argumentos 2>&1 | Tee-Object -FilePath (Join-Path $PSScriptRoot ($marca+'-'+$Modo+'.log'))
 $codigo=$LASTEXITCODE
 $comando='npm.cmd '+($argumentos -join ' ')
} else {
 Set-Location (Join-Path $repoLocal 'backend')
 if($Modo -eq 'dirigido') {$argumentos=@('test','--tests','*Fase4IT','--tests','*Fase4ReglasTest','--tests','*Backend24IT')}
 elseif($Modo -eq 'jacoco') {$argumentos=@('jacocoTestReport','jacocoTestCoverageVerification','-x','test')}
 else {$argumentos=@('check')}
 $argumentos+=@('--console=plain','--no-daemon')
 & .\gradlew.bat @argumentos 2>&1 | Tee-Object -FilePath (Join-Path $PSScriptRoot ($marca+'-'+$Modo+'.log'))
 $codigo=$LASTEXITCODE
 $comando='.\gradlew.bat '+($argumentos -join ' ')
}
@{comando=$comando;salida=$codigo;fecha=$marca;almacenAislado=$env:ALMACEN_LOCAL_DIR;googleReal=$false} | ConvertTo-Json | Set-Content -Encoding UTF8 (Join-Path $PSScriptRoot ($marca+'-'+$Modo+'.json'))
exit $codigo
