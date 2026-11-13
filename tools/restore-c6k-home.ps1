[CmdletBinding(SupportsShouldProcess)]
param(
    [Parameter(Mandatory)][ValidatePattern('^192\.168\.0\.107:[0-9]{1,5}$')][string]$Serial,
    [switch]$Apply
)
$ErrorActionPreference = 'Stop'
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$stock = 'com.google.android.apps.tv.launcherx'
$component = "$stock/.home.HomeActivity"
function Read-Tv {
    $result = & $adb -s $Serial shell @args
    if ($LASTEXITCODE -ne 0) { throw 'ADB interrumpido. Revisa el estado antes de volver a ejecutar.' }
    $result
}
if ((Read-Tv getprop ro.build.fingerprint) -ne 'TCL/G08_4K_GB/G08:14/UTT2.250416.001/AU02:user/release-keys') { throw 'Firmware distinto.' }
if ((Read-Tv am get-current-user) -ne '0') { throw 'Usuario distinto de 0.' }
if ('package:com.google.android.apps.tv.launcherx' -notin (Read-Tv pm list packages -e --user 0 $stock)) { throw 'Google TV no figura habilitado.' }
$resolve = @('cmd','package','resolve-activity','--brief','--user','0','-a','android.intent.action.MAIN','-c','android.intent.category.HOME')
Read-Tv @resolve
Read-Tv dumpsys role | Select-String 'user_id|name=android.app.role.HOME' -Context 0,2
$restore = @('cmd','package','set-home-activity','--user','0',$component)
Write-Output "adb -s $Serial shell $($restore -join ' ')"
if ($Apply -and $PSCmdlet.ShouldProcess($Serial, 'Restaurar Google TV como HOME')) {
    $result = Read-Tv @restore
    $result
    if ('Success' -notin $result) { throw 'Android no confirmó la restauración.' }
    if ($component -notin (Read-Tv @resolve)) { throw 'El resolver no coincide con Google TV.' }
    Read-Tv dumpsys role | Select-String 'user_id|name=android.app.role.HOME' -Context 0,2
    Write-Output 'Comprueba el titular del rol del usuario 0 y pulsa Home en el mando.'
}
