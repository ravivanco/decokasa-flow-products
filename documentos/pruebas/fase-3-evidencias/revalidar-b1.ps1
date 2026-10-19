param([ValidateSet('reproductores','arnes','check','cobertura')][string]$Etapa)
$ErrorActionPreference='Continue'
$repoB1=[System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\..'))
$salidaB1=Join-Path $PSScriptRoot 'revalidacion-B1'
New-Item -ItemType Directory -Force $salidaB1 | Out-Null
Set-Location (Join-Path $repoB1 'backend')
$env:GOOGLE_OAUTH_HABILITADA='false'
$env:GOOGLE_DRIVE_HABILITADA='false'
$env:GOOGLE_GMAIL_HABILITADA='false'
$env:COPIA_DRIVE_AUTOMATICA='false'
$env:CORREO_AUTOMATICO='false'
$env:SPRING_CONFIG_IMPORT=''
$env:SEMILLA_HABILITADA='false'
# Los contextos heredados que no sobrescriben el disco también quedan aislados.
$discoB1=Join-Path ([System.IO.Path]::GetTempPath()) ('decokasa-revalidacion-b1-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force $discoB1 | Out-Null
$env:ALMACEN_LOCAL_DIR=$discoB1
# Fallar cerrado si alguna prueba omite su Testcontainer; nunca usar una base de desarrollo.
$env:DB_URL='jdbc:postgresql://127.0.0.1:1/b1-sin-base-compartida'
$env:DB_USUARIO='b1-ficticio'
$env:DB_CONTRASENA='b1-ficticia-sin-uso-real'
switch ($Etapa) {
    'reproductores' { $argumentosB1=@('test','--tests','*DriveRecuperacionB1IT.cerrojoOcupadoNoHaceRedNiConsumeReintentoAdicional') }
    'arnes' { $argumentosB1=@('-I','..\documentos\pruebas\fase-3-evidencias\archivos-aislados.init.gradle','archivosB1Aislados') }
    'check' { $argumentosB1=@('check') }
    'cobertura' { $argumentosB1=@('jacocoTestReport','-x','test') }
}
$argumentosB1+=@('--console=plain','--no-daemon')
$marcaB1=Get-Date -Format 'yyyyMMdd-HHmmss'
$logB1=Join-Path $salidaB1 ($marcaB1+'-'+$Etapa+'.log')
& .\gradlew.bat @argumentosB1 2>&1 | Tee-Object -FilePath $logB1
$codigoB1=$LASTEXITCODE
@{etapa=$Etapa;comando=('.\gradlew.bat '+($argumentosB1 -join ' '));salida=$codigoB1;fecha=$marcaB1;discoAislado=$discoB1} | ConvertTo-Json | Set-Content -Encoding UTF8 (Join-Path $salidaB1 ($marcaB1+'-'+$Etapa+'.json'))
exit $codigoB1
