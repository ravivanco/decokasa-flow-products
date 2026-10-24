param([ValidateSet('Completa','Diagnostico','Defecto')][string]$Modo='Diagnostico')
$ErrorActionPreference='Continue'
$repo25=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\..\..'))
$init25=Join-Path $PSScriptRoot 'local.init.gradle'
$argumentos25=@('-I',$init25)
if($Modo -ne 'Completa'){$argumentos25+='-PdiagnosticoSinAsistente'}
if($Modo -eq 'Completa'){
    $argumentos25+=@('compileJava','test','--tests','*ReglasArquitecturaTest')
}elseif($Modo -eq 'Defecto'){
    $argumentos25+=@('test','--tests','*CarpetasDriveAdr25IT.eventoIncoherenteFallaSinCrearCarpetas')
}else{
    $argumentos25+=@('test','--tests','*CarpetasDriveAdr25IT','--tests','*CarpetasDriveProtocoloAdr25Test',
        '--tests','*ReglasArquitecturaTest','--tests','*FronterasFaseDosTest',
        '--tests','*DriveRecuperacionB1IT','--tests','*GoogleDriveProtocoloB1Test','--tests','*AplicacionArrancaIT')
}
$argumentos25+=@('--offline','--console=plain','--no-daemon')
$log25=Join-Path $PSScriptRoot ((Get-Date -Format 'yyyyMMdd-HHmmss')+'-'+$Modo+'.log')
Push-Location (Join-Path $repo25 'backend')
try{
    & .\gradlew.bat @argumentos25 2>&1 | Tee-Object -FilePath $log25
    $salida25=$LASTEXITCODE
}finally{Pop-Location}
exit $salida25
