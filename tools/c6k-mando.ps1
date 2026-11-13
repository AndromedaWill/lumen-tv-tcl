param([ValidateSet('Start','Stop')][string]$Action = 'Start',[string]$Serial = '192.168.0.107:5555')
$ErrorActionPreference = 'Stop'
if ($Serial -notmatch '^192\.168\.0\.107:[0-9]{1,5}$') { throw 'Endpoint inesperado' }
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb connect $Serial
$firmware = & $adb -s $Serial shell getprop ro.build.fingerprint
if ($firmware -ne 'TCL/G08_4K_GB/G08:14/UTT2.250416.001/AU02:user/release-keys') { throw 'TV no disponible o firmware distinto' }
if ($Action -eq 'Stop') {
    & $adb -s $Serial shell 'd=/data/local/tmp/tcl-remote-session; [ -r "$d/owner.pid" ] || exit 0; p=$(cat "$d/owner.pid"); case "$p" in ""|*[!0-9]*) exit 2;; esac; tr "\000" " " < "/proc/$p/cmdline" | grep -Fxq "sh /data/local/tmp/c6k-keys-on-tv.sh --session " || exit 3; child=$(ps -A -o PID,PPID,ARGS | awk -v p="$p" ''$2==p && $3=="getevent" && $4=="-lt" && $5~/^\/dev\/input\/event[0-9]+$/ {print $1}''); case "$child" in ""|*[!0-9]*) exit 4;; esac; kill "$child"'
    if ($LASTEXITCODE) { throw 'No se pudo identificar el lector con seguridad' }
    return
}
& $adb -s $Serial push (Join-Path $PSScriptRoot 'c6k-keys-on-tv.sh') /data/local/tmp/c6k-keys-on-tv.sh
if ($LASTEXITCODE) { throw 'Copia fallida' }
& $adb -s $Serial shell 'sh -n /data/local/tmp/c6k-keys-on-tv.sh && (nohup sh /data/local/tmp/c6k-keys-on-tv.sh --session </dev/null >/data/local/tmp/c6k-keys-session.log 2>&1 &)'
if ($LASTEXITCODE) { throw 'Inicio fallido' }
Start-Sleep -Milliseconds 700
& $adb -s $Serial shell 'd=/data/local/tmp/tcl-remote-session; p=$(cat "$d/owner.pid" 2>/dev/null); case "$p" in ""|*[!0-9]*) cat /data/local/tmp/c6k-keys-session.log; exit 1;; esac; kill -0 "$p" && echo "Lector activo: $p"'
if ($LASTEXITCODE) { throw 'El lector no está activo' }
