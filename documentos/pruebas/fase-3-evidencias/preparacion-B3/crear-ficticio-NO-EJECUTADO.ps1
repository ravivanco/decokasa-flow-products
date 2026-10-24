$archivoEnsayoB3 = 'C:\Users\Richard\.decokasa\ensayos\3b3\ficticio-3b3-100MiB.bin'
$flujoEnsayoB3 = [IO.File]::Open($archivoEnsayoB3, [IO.FileMode]::CreateNew,
    [IO.FileAccess]::Write, [IO.FileShare]::None)
try { $flujoEnsayoB3.SetLength(104857600); $flujoEnsayoB3.Flush($true) }
finally { $flujoEnsayoB3.Dispose() }
if ((Get-Item -LiteralPath $archivoEnsayoB3).Length -ne 104857600) { throw 'Tamaño incorrecto' }
Get-FileHash -LiteralPath $archivoEnsayoB3 -Algorithm SHA256