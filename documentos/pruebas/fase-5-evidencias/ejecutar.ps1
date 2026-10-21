param([ValidateSet('baseline','dirigido','check','cobertura')][string]$Modo)
$ErrorActionPreference='Continue'
$repoLocal=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\..'))
Set-Location (Join-Path $repoLocal 'backend')
$env:GOOGLE_OAUTH_HABILITADA='false'
$env:GOOGLE_DRIVE_HABILITADA='false'
$env:GOOGLE_GMAIL_HABILITADA='false'
$env:COPIA_DRIVE_AUTOMATICA='false'
$env:CORREO_AUTOMATICO='false'
$env:VENCIMIENTOS_AUTOMATICO='false'
$env:SPRING_CONFIG_IMPORT=''
$env:SEMILLA_HABILITADA='false'
$env:DB_URL='jdbc:postgresql://127.0.0.1:1/sin-base-compartida'
$env:DB_USUARIO='prueba-ficticia'
$env:DB_CONTRASENA='prueba-ficticia'
$env:ALMACEN_LOCAL_DIR=Join-Path ([IO.Path]::GetTempPath()) ('decokasa-fase5-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $env:ALMACEN_LOCAL_DIR -Force | Out-Null
if($Modo -eq 'dirigido') {$argumentos=@('test','--tests','*Fase5*','--tests','*MigracionesIT','--tests','*V2RestriccionesIT','--tests','*MigracionesFase4IT')}
elseif($Modo -eq 'cobertura') {$argumentos=@('jacocoTestReport','jacocoTestCoverageVerification','-x','test')}
else {$argumentos=@('check')}
$argumentos+=@('--console=plain','--no-daemon')
$marca=Get-Date -Format 'yyyyMMdd-HHmmss'
$inicioEjecucion=(Get-Date).ToUniversalTime()
& .\gradlew.bat @argumentos 2>&1 | Tee-Object -FilePath (Join-Path $PSScriptRoot ($marca+'-'+$Modo+'.log'))
$codigo=$LASTEXITCODE
@{comando=('.\gradlew.bat '+($argumentos -join ' '));salida=$codigo;fecha=$marca;googleReal=$false;deteccionAutomatica=$false;almacenAislado=$env:ALMACEN_LOCAL_DIR} | ConvertTo-Json | Set-Content -Encoding UTF8 (Join-Path $PSScriptRoot ($marca+'-'+$Modo+'.json'))
$xmlNuevos=@(Get-ChildItem 'build/test-results/test/TEST-*.xml' -ErrorAction SilentlyContinue | Where-Object {$_.LastWriteTimeUtc -ge $inicioEjecucion})
if($xmlNuevos.Count) {Copy-Item 'build/test-results/test' (Join-Path $PSScriptRoot ($marca+'-'+$Modo+'-junit')) -Recurse}
if((Test-Path 'build/reports/jacoco/test/jacocoTestReport.xml') -and (Get-Item 'build/reports/jacoco/test/jacocoTestReport.xml').LastWriteTimeUtc -ge $inicioEjecucion) {Copy-Item 'build/reports/jacoco/test/jacocoTestReport.xml' (Join-Path $PSScriptRoot ($marca+'-'+$Modo+'-jacoco.xml'))}
exit $codigo
