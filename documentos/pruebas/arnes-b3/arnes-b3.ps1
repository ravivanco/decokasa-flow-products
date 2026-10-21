param(
    [ValidateSet('Estado','Comprobar','PrepararClasspath','Iniciar')][string]$Accion='Estado',
    [string]$DirectorioEnsayo
)
$ErrorActionPreference='Stop'
$repoArnesB3=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\..'))
if((git -C $repoArnesB3 branch --show-current) -ne 'fase-5-seguimiento'){throw 'Rama inesperada'}
if($Accion -eq 'Estado'){
    Write-Output 'Arnes opt-in preparado. No inicia JVM, Docker, OAuth, copia ni correo sin una accion explicita.'
    exit 0
}
if($Accion -in @('Comprobar','PrepararClasspath')){
    $tareasB3=if($Accion -eq 'Comprobar'){@('b3ComprobarGuardas','b3Classpath')}else{@('b3Classpath')}
    Push-Location (Join-Path $repoArnesB3 'backend')
    try{
        & .\gradlew.bat -I (Join-Path $PSScriptRoot 'b3.init.gradle') @tareasB3 --offline --console=plain --no-daemon
        exit $LASTEXITCODE
    }finally{Pop-Location}
}
if(-not $DirectorioEnsayo -or -not [IO.Path]::IsPathRooted($DirectorioEnsayo)){throw 'Indica directorio externo absoluto existente con nombre UUID y ACL privada'}
$classpathB3=Join-Path $PSScriptRoot '.tmp\classpath.txt'
if(-not (Test-Path -LiteralPath $classpathB3)){throw 'Ejecuta primero -Accion Comprobar; no se compila ni inicia implicitamente'}
# Java exige consola real, frase de inicio y otras confirmaciones para cada operacion.
# Ningun secreto viaja en argumentos; no usar Start-Process ni registros de consola de autenticacion.
$argumentosJavaB3=Join-Path $PSScriptRoot '.tmp\java-args.txt'
$cpJavaB3=[IO.File]::ReadAllText($classpathB3,[Text.Encoding]::UTF8).Replace('\','/')
$raizJavaB3=$DirectorioEnsayo.Replace('\','/')
$repoJavaB3=$repoArnesB3.Replace('\','/')
if($raizJavaB3.Contains('"')){throw 'Ruta invalida'}
$textoJavaB3="-cp`n`"$cpJavaB3`"`ncom.decokasa.ensayob3.ArnesGoogleB3`n`"$raizJavaB3`"`n`"$repoJavaB3`"`n"
[IO.File]::WriteAllText($argumentosJavaB3,$textoJavaB3,[Text.UTF8Encoding]::new($false))
& java ('@'+$argumentosJavaB3)
exit $LASTEXITCODE
