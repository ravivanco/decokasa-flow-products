param([ValidateSet('baseline','nuevas','archivos','check','cobertura')][string]$Etapa)
$ErrorActionPreference='Continue'
$repoLocal=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\..\..'))
Set-Location (Join-Path $repoLocal 'backend')
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
$env:ALMACEN_LOCAL_DIR=Join-Path ([IO.Path]::GetTempPath()) ('decokasa-local-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $env:ALMACEN_LOCAL_DIR -Force | Out-Null
switch($Etapa) {
 'baseline' { $argumentos=@('check') }
 'nuevas' { $argumentos=@('test','--tests','*CuotasLocalesIT','--tests','*CorreoReintentosLocalesIT','--tests','*GmailProtocoloLocalTest','--tests','*MigracionesArchivosB1IT') }
 'archivos' { $argumentos=@('test','--tests','com.decokasa.seguimiento.archivos.*') }
 'check' { $argumentos=@('check') }
 'cobertura' { $argumentos=@('jacocoTestReport','-x','test') }
}
$argumentos+=@('--console=plain','--no-daemon')
$marca=Get-Date -Format 'yyyyMMdd-HHmmss'
$log=Join-Path $PSScriptRoot ($marca+'-'+$Etapa+'.log')
& .\gradlew.bat @argumentos 2>&1 | Tee-Object -FilePath $log
$codigo=$LASTEXITCODE
@{etapa=$Etapa;comando=('.\gradlew.bat '+($argumentos -join ' '));salida=$codigo;fecha=$marca;almacenAislado=$env:ALMACEN_LOCAL_DIR} | ConvertTo-Json | Set-Content -Encoding UTF8 (Join-Path $PSScriptRoot ($marca+'-'+$Etapa+'.json'))
$reporte=Join-Path $repoLocal 'backend\build\reports\jacoco\test\jacocoTestReport.xml'
if(Test-Path -LiteralPath $reporte){Copy-Item -LiteralPath $reporte -Destination (Join-Path $PSScriptRoot ($marca+'-'+$Etapa+'-jacoco.xml'))}
exit $codigo
