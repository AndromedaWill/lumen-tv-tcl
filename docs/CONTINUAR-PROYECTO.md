# Continuar el proyecto después de la limpieza

Limpieza local del 7 de noviembre de 2026. Se liberaron 1.855.073.348 bytes, aproximadamente 1,73 GiB. El proyecto pasó de 2,16 GiB a unos 442 MiB.

## Archivos conservados

- Código fuente completo, cambios locales sin confirmar, historial Git, licencias, documentación y pruebas de código.
- `launcher/app/src/main/assets/personal` contiene las seis imágenes necesarias para construir el APK desde un clon del repositorio. La copia original en `.dev/personal-assets` se conserva localmente.
- `.dev/jdk17` y el SDK externo de Android permanecen disponibles para compilar. Se mantiene la clave de firma existente en su ubicación original. No cambiar la firma ni el identificador del paquete.
- `artifacts/conservado/ultimo-instalado-mando-autonomo.apk` es el último APK instalado y aprobado según los registros de la sesión, sin nueva consulta de la TV.
- `artifacts/conservado/anterior-panel-entradas.apk` es la versión anterior de respaldo.
- `artifacts/conservado/candidato-local-lumen-tv.apk` es la compilación local posterior con el nombre Lumen TV. No consta instalada en la TV.
- `artifacts/conservado/apk-sha256.json` registra los hashes de las copias y referencias visuales. La referencia-1 estaba vacía y se eliminó.
- Se conservaron los inventarios originales de HOME, paquetes y debloat, los scripts de restauración y los registros de texto de pruebas.

## Lo eliminado

APK históricos redundantes, capturas y vídeos de pruebas antiguos, salidas de compilación, caché local Gradle, descargas incompletas del emulador y copias temporales del SDK/referencias. El inventario está en `artifacts/conservado/archivos-eliminados.txt`.

Los enlaces antiguos a esos APK y capturas ya no estarán disponibles. Las cifras históricas documentadas se conservan como antecedentes. No volver a presentarlas como validación del APK actual.

## Compilar de nuevo

Usar Java 17 de `.dev/jdk17`, `ANDROID_HOME` apuntando al SDK existente en `C:\Users\milmil\AppData\Local\Android\Sdk`, y desde `launcher` ejecutar el wrapper con `:app:assembleRelease :app:testDebugUnitTest :app:lintRelease`. La primera compilación regenerará las carpetas eliminadas.

La limpieza no modificó el código ni implementó las funciones propuestas de exportación, búsqueda o alias HDMI. Esas mejoras siguen pendientes. Parte del código actual conserva cambios locales anteriores al último commit y debe revisarse antes del siguiente checkpoint.

## TV y recuperación

Según la última sesión comprobada, el paquete `pe.tclhome.prototype` es HOME real y launcherx/setupwraith están deshabilitados. No borrar la aplicación del televisor sin restaurar previamente Google TV. Los respaldos permanecen en `artifacts/home-c6k-20261103`, y el procedimiento requiere revisar el endpoint ADB vigente y obtener autorización antes de escribir.

Esta limpieza no se conectó al televisor, no modificó HOME ni paquetes y no borró claves de firma, archivos personales originales o herramientas externas al proyecto.
