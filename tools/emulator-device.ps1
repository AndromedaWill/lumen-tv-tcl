param(
    [Parameter(Mandatory=$true)][ValidatePattern('^emulator-[0-9]+$')][string]$Serial,
    [Parameter(Mandatory=$true)][ValidateSet('Install','Launch','Capture','Metrics')][string]$Action,
    [string]$Apk = "$PSScriptRoot\..\artifacts\tcl-home-prototype.apk",
    [ValidateSet('pe.tclhome.prototype','com.gathin.tvhome')][string]$Package = 'pe.tclhome.prototype',
    [string]$Output = "$PSScriptRoot\..\artifacts\home.png"
)
$ErrorActionPreference = 'Stop'
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
if ((& $adb -s $Serial shell getprop ro.kernel.qemu).Trim() -ne '1') { throw 'Refusing a non-emulator device.' }
switch ($Action) {
    Install { & $adb -s $Serial install -r $Apk }
    Launch { & $adb -s $Serial shell am start -W -n "$Package/com.gathin.tvhome.MainActivity" }
    Capture {
        # pull preserves PNG bytes under Windows PowerShell 5.1 (unlike binary redirection).
        & $adb -s $Serial shell screencap -p /sdcard/tcl-home-capture.png
        if ($LASTEXITCODE -ne 0) { throw 'Screenshot failed.' }
        & $adb -s $Serial pull /sdcard/tcl-home-capture.png $Output
    }
    Metrics {
        & $adb -s $Serial shell dumpsys meminfo $Package
        & $adb -s $Serial shell dumpsys gfxinfo $Package
        & $adb -s $Serial shell dumpsys cpuinfo
    }
}
if ($LASTEXITCODE -ne 0) { throw "ADB failed: $LASTEXITCODE" }
