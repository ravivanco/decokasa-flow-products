$fallidos=@()
foreach($paso in @('lint','test','coverage','build')) {
 & powershell.exe -NoProfile -File (Join-Path $PSScriptRoot 'ejecutar.ps1') $paso
 if($LASTEXITCODE -ne 0) {$fallidos+=$paso}
}
if($fallidos.Count) {Write-Output ('Comandos fallidos: '+($fallidos -join ', '));exit 1}
exit 0
