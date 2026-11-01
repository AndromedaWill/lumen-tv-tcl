# Compilación reproducible en Windows

PowerShell, Git, Python 3 y conexión HTTPS para la primera descarga. No se necesita TV ni ADB para compilar. Nunca usar `build-and-install.ps1` para este flujo: también cambia el dispositivo.

## Versiones

JDK Temurin 17.0.20.1+1 local en `.dev/jdk17/`; Android platform 34 revisión 3 (ext7), build-tools 34.0.0; AGP 8.5.2; Gradle 8.9 mediante wrapper. SDK en `%LOCALAPPDATA%/Android/Sdk`. Android Studio instalado usa Java 25, por eso se fija JDK 17 solo en el proceso de compilación. No cambiar JAVA_HOME global.

Descargas oficiales: [Temurin](https://adoptium.net/temurin/releases/?version=17), [Android command-line tools](https://developer.android.com/studio#command-line-tools-only). Desde SDK Manager instalar `platforms;android-34`, `build-tools;34.0.0`, `system-images;android-34;android-tv;x86`. Conservar licencias aceptadas localmente; no versionar SDK, JDK, credenciales ni keystore.

Checksums usados en esta sesión:

| Archivo oficial | Algoritmo | Digest |
|---|---|---|
| gradle-8.9-bin.zip | SHA-256 | d725d707bfabd4dfdc958c624003b3c80accc03f7037b5122c4b1d0ef15cecab |
| OpenJDK17U-jdk_x64_windows_hotspot_17.0.20.1_1.zip | SHA-256 | e53a79c3c3d86865bd7e787903884331068e71321714ffd44f145785affc7cb0 |
| commandlinetools-win-15859902_latest.zip | SHA-256 | 90ae805d20434428bffcb699c290860f19bb5f66a67e6b330067e3de801fb04a |
| platform-34-ext7_r03.zip | SHA-1 del catálogo SDK | 1f2e9478d6a7601425ceaa553311dc43191f103d |
| build-tools_r34-windows.zip | SHA-1 del catálogo SDK | 62cfde1b6fcc3ad12a4d2ba1b537e752768bfd47 |
| android-tv/x86-34_r03.zip | SHA-1 del catálogo SDK | 8de1dcb502b0b4080e6f363828db64e609b3aa2a |

Los catálogos se descargan por HTTPS de dl.google.com. SHA-1 aquí es la comprobación de integridad publicada por SDK Manager, no una firma propia ni una elección de criptografía para el producto. Los APK se registran con SHA-256. El wrapper comprueba su distribución; si Java no progresa descargándola, descargar el mismo ZIP por HTTPS, verificar su SHA-256 y precargar su directorio de caché, sin modificar el wrapper.

## Construir

Desde raíz:

```powershell
.\tools\build-prototype.ps1
```

Ejecuta tests JVM, lintRelease y assembleRelease. Salida: `artifacts/tcl-home-prototype.apk`. Firma debug local, identificador `pe.tclhome.prototype`; no apta como release firmada definitiva. Copia del baseline original y logs en artifacts, fuera de Git. Para reproducir el original usar un checkout separado de `046da15`, mismas herramientas y tareas Gradle, sin cambios en fuentes.

## Emulador y capturas

Crear AVD Android TV API 34 x86, 2 GB RAM, cuatro núcleos, aceleración WHPX. Dos perfiles de hardware independientes: 1920×1080/density 320 y 1280×720/density 213. Sus config.ini fijan resolución: no ejecutar `wm size` o `wm density` sobre la TV. No reutilizar el AVD personal Medium Phone.

```powershell
.\tools\emulator-device.ps1 -Serial emulator-5554 -Action Install
.\tools\emulator-device.ps1 -Serial emulator-5554 -Action Launch
.\tools\emulator-device.ps1 -Serial emulator-5554 -Action Capture -Output .\artifacts\home-1080.png
```

El script rechaza seriales de TV y verifica `ro.kernel.qemu=1`. No selecciona Inicio, activa accesibilidad ni concede permisos. Para cerrar el prototipo basta Inicio o el launcher del AVD; para cerrar el AVD, usar su ventana o `adb -s emulator-5554 emu kill` únicamente tras verificar el serial.

Las aplicaciones de `tools/fixtures` son APK de prueba instalados de verdad y descubiertos por PackageManager. Las capturas deben identificarlas como tales. No confundir el AVD con firmware TCL ni con una reproducción 4K del panel físico.

Para este equipo, la validación final usa `-gpu host -feature -Vulkan` y WHPX, sin dos AVD simultáneos. SwiftShader presentó un ANR en el baseline, por lo que esa corrida no sirve para comparar. Registrar modo GPU, versión del emulador, PSS y jank junto a cada resultado. No extrapolar rendimiento del PC a la TCL.

El perfil Android TV 720p creó una partición de datos de 6 GiB, que el emulador volvió a imponer al iniciar. El primer arranque abortaba por espacio. Se liberaron 2.391.192.778 bytes de archivos temporales de descarga creados por esta sesión (ZIP y partes), conservando SDK/JDK instalados, fuentes y artifacts. El AVD TCL_Home_720_v2 arrancó a 1280×720/density 213 con el tamaño exigido; su primer arranque tomó 112,5 s. Esto no es tiempo de arranque del APK ni una medida de la TV.
