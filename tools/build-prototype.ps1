param(
    [string]$JavaHome = "$PSScriptRoot\..\.dev\jdk17\jdk-17.0.20.1+1",
    [string]$SdkRoot = "$env:LOCALAPPDATA\Android\Sdk",
    [string]$WallpaperSource = "$env:USERPROFILE\Documents\TCL Walpaper"
)
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path "$PSScriptRoot\..").Path
if (!(Test-Path "$JavaHome\bin\java.exe")) { throw 'Install JDK 17; see docs/ENTORNO-WINDOWS.md.' }
if (!(Test-Path "$SdkRoot\platforms\android-34\android.jar")) { throw 'SDK platform 34 is missing.' }
$version = (& "$JavaHome\bin\java.exe" --version | Out-String)
if ($version -notmatch '(openjdk|java) 17\.') { throw 'This build is pinned to Java 17.' }
# Private user images are build inputs, never rewritten or committed. Exact originals are copied.
$wallpaperAssets = "$repo\.dev\personal-assets\personal"
if (Test-Path -LiteralPath $WallpaperSource) {
    foreach ($phaseNumber in 1..6) {
        $sourceImage = Join-Path $WallpaperSource "$phaseNumber.jpg"
        if (!(Test-Path -LiteralPath $sourceImage)) { throw "Missing phase: $sourceImage" }
    }
    New-Item -ItemType Directory -Force $wallpaperAssets | Out-Null
    foreach ($phaseNumber in 1..6) {
        Copy-Item -LiteralPath (Join-Path $WallpaperSource "$phaseNumber.jpg") -Destination (Join-Path $wallpaperAssets "$phaseNumber.jpg")
    }
}
$env:JAVA_HOME = (Resolve-Path $JavaHome).Path
$env:ANDROID_HOME = $SdkRoot
$env:JAVA_TOOL_OPTIONS = '-Djava.net.preferIPv4Stack=true -Dhttps.protocols=TLSv1.2'
$argsBuild = @('-p', "$repo\launcher", '--no-daemon', 'testReleaseUnitTest', 'lintRelease', 'assembleRelease')
$argsBuild += '-PlauncherApplicationId=pe.tclhome.prototype'
& "$repo\launcher\gradlew.bat" @argsBuild
if ($LASTEXITCODE -ne 0) { throw "Gradle failed: $LASTEXITCODE" }
$out = New-Item -ItemType Directory -Force "$repo\artifacts"
$name = 'tcl-home-prototype.apk'
Copy-Item -LiteralPath "$repo\launcher\app\build\outputs\apk\release\app-release.apk" -Destination "$out\$name"
Get-FileHash -Algorithm SHA256 "$out\$name"
# Deliberately no ADB, installation, Home selection, permissions or package changes.
