param([ValidateSet('frontend','backend')][string]$Etapa)
$repoB2=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\..\..'))
if ((git -C $repoB2 branch --show-current) -ne 'fase-3-drive-gmail') { throw 'Rama inesperada' }
if($Etapa -eq 'frontend') {
 Copy-Item (Join-Path $PSScriptRoot 'ArchivosYPermisos.test.tsx') (Join-Path $repoB2 'frontend\src\components\pedidos\ArchivosYPermisos.test.tsx')
 Copy-Item (Join-Path $PSScriptRoot 'App.test.tsx') (Join-Path $repoB2 'frontend\src\App.test.tsx')
 Copy-Item (Join-Path $PSScriptRoot 'sin-demo.test.ts') (Join-Path $repoB2 'frontend\src\test\sin-demo.test.ts')
 Copy-Item (Join-Path $PSScriptRoot 'GmailNotificationsView.test.tsx') (Join-Path $repoB2 'frontend\src\components\GmailNotificationsView.test.tsx')

 Set-Location (Join-Path $repoB2 'frontend')
 npm.cmd run lint *> (Join-Path $PSScriptRoot 'lint-final.log'); $l=$LASTEXITCODE
 npm.cmd test *> (Join-Path $PSScriptRoot 'test-final.log'); $t=$LASTEXITCODE
 npm.cmd run build *> (Join-Path $PSScriptRoot 'build-final.log'); $b=$LASTEXITCODE
 @{lint=$l;test=$t;build=$b} | ConvertTo-Json | Set-Content (Join-Path $PSScriptRoot 'resultados-finales.json')
} else {
 Copy-Item (Join-Path $PSScriptRoot 'CorreoB2IT.java') (Join-Path $repoB2 'backend\src\test\java\com\decokasa\seguimiento\archivos\CorreoB2IT.java')
 Copy-Item (Join-Path $PSScriptRoot 'Backend24IT.java') (Join-Path $repoB2 'backend\src\test\java\com\decokasa\seguimiento\pedidos\Backend24IT.java')
 Set-Location (Join-Path $repoB2 'backend')
 $env:GOOGLE_OAUTH_HABILITADA='false';$env:GOOGLE_DRIVE_HABILITADA='false';$env:GOOGLE_GMAIL_HABILITADA='false'
 $env:COPIA_DRIVE_AUTOMATICA='false';$env:CORREO_AUTOMATICO='false';$env:SPRING_CONFIG_IMPORT='';$env:SEMILLA_HABILITADA='false'
 $env:DB_URL='jdbc:postgresql://127.0.0.1:1/b2-aislado';$env:DB_USUARIO='ficticio';$env:DB_CONTRASENA='ficticia'
 $env:ALMACEN_LOCAL_DIR=Join-Path ([IO.Path]::GetTempPath()) ('b2-'+[Guid]::NewGuid().ToString('N'))
 .\gradlew.bat test --tests '*CorreoB2IT' --tests '*Backend24IT.b2*' --console=plain --no-daemon *> (Join-Path $PSScriptRoot 'backend.log')
 @{salida=$LASTEXITCODE;comando='gradlew.bat test --tests *CorreoB2IT --tests *Backend24IT.b2* --console=plain --no-daemon'} | ConvertTo-Json | Set-Content (Join-Path $PSScriptRoot 'backend.json')
}
