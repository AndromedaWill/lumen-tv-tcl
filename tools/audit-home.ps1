param(
    [Parameter(Mandatory=$true)][ValidatePattern('^[A-Za-z0-9_.:-]+$')][string]$Serial,
    [string]$Output = "$PSScriptRoot\..\artifacts\home-audit"
)
# Read-only: no connect, install, set-home, role mutation, settings put or package changes.
$ErrorActionPreference = 'Stop'
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
function Read-Adb([string[]]$ReadArgs) {
    $ErrorActionPreference = 'Continue'
    $lines = @(& $adb -s $Serial @ReadArgs 2>&1 | ForEach-Object { "$_" })
    return @{ exit = $LASTEXITCODE; text = ($lines -join "`n") }
}
$state = Read-Adb @('get-state')
if ($state.exit -ne 0 -or $state.text.Trim() -ne 'device') { throw 'Explicit device is not available/authorized.' }
$userRead = Read-Adb @('shell','am','get-current-user')
$userId = $userRead.text.Trim()
if ($userRead.exit -ne 0 -or $userId -notmatch '^\d+$') { throw 'Cannot identify current Android user.' }
$report = [ordered]@{ schema=1; serial=$Serial; capturedUtc=[DateTime]::UtcNow.ToString('o'); user=$userId }
$properties = [ordered]@{}
foreach ($key in @('ro.product.manufacturer','ro.product.model','ro.product.device','ro.product.name',
        'ro.build.version.sdk','ro.build.version.release','ro.build.display.id','ro.build.fingerprint')) {
    $properties[$key] = Read-Adb @('shell','getprop',$key)
}
$report.properties = $properties
$report.home = Read-Adb @('shell','cmd','package','resolve-activity','--brief','--user',$userId,
    '-a','android.intent.action.MAIN','-c','android.intent.category.HOME')
$report.candidates = Read-Adb @('shell','cmd','package','query-activities','--brief','--user',$userId,
    '-a','android.intent.action.MAIN','-c','android.intent.category.HOME')
$report.roleHome = Read-Adb @('shell','cmd','role','get-role-holders','--user',$userId,'android.app.role.HOME')
$report.accessibilityServices = Read-Adb @('shell','settings','--user',$userId,'get','secure','enabled_accessibility_services')
$report.accessibilityEnabled = Read-Adb @('shell','settings','--user',$userId,'get','secure','accessibility_enabled')
$report.prototype = Read-Adb @('shell','dumpsys','package','pe.tclhome.prototype')
$original = @($report.home.text -split "`n" | Where-Object { $_ -match '^[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+$' }) | Select-Object -Last 1
$report.originalHomeComponent = $original
if ($original) {
    $originalPackage = ($original -split '/')[0]
    $report.originalPackage = Read-Adb @('shell','dumpsys','package',$originalPackage)
}
$outDir = New-Item -ItemType Directory -Force -Path $Output
$report | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath "$outDir\home-audit.json" -Encoding UTF8
if ($original -and $original -notmatch '^(android|com.android.tv.settings)/') {
    @(
        'PLAN ONLY. These commands are NOT executed. Requires separate approval before changing Home.',
        "Original Home: $original; Android user: $userId",
        'Preferred recovery: Settings > Default apps > Home > the original launcher.',
        'ADB alternative to review after identifying the actual firmware:',
        "adb -s $Serial shell cmd package set-home-activity --user $userId $original",
        "adb -s $Serial shell cmd package resolve-activity --brief --user $userId -a android.intent.action.MAIN -c android.intent.category.HOME",
        'Expected: the resolver returns the original component above. Then verify real Home, app exit and reboot.',
        'If RoleManager was changed, restore its saved holder as a separate, reviewed operation.',
        'No stock package was disabled by this audit; no enable/disable operation is needed.'
    ) | Set-Content -LiteralPath "$outDir\RESTORE-HOME-PLAN.txt" -Encoding UTF8
}
Write-Output "Read-only audit saved locally. Current Home: $original. No device settings changed."
