# Lumen TV para TCL C6K

Checkpoint de código preparado el 12 de noviembre de 2026. Fork basado en Apple TV Launcher y CleanYourTV. Identificador Android `pe.tclhome.prototype`, nombre visible Lumen TV. Se conservan licencias y atribuciones de las bases.

## Compilación desde un clon

Instalar Java 17, Android SDK Platform 34 y Build Tools 34.0.0. Configurar JAVA_HOME y ANDROID_HOME para esas instalaciones. El wrapper Gradle 8.9 verifica el SHA-256 de su distribución.

Desde PowerShell, en la carpeta launcher ejecutar `./gradlew.bat :app:assembleRelease :app:testDebugUnitTest :app:lintRelease`. En Linux usar `./gradlew`.

Las seis imágenes predeterminadas están en app/src/main/assets/personal. El proyecto puede compilarlas sin la carpeta .dev del equipo original. Licencias de los demás recursos en app/src/main/assets/licenses y documentación del sistema de diseño.

El APK se genera en app/build/outputs/apk/release/app-release.apk. Para actualizar una instalación existente debe firmarse con su mismo certificado. El repositorio excluye claves y credenciales. Una clave debug nueva de otro equipo no permitirá actualizar la instalación actual. Conservar la clave original por separado.

## Estado de uso

El propietario confirmó Inicio real y persistencia después de reiniciar en TCL G08/AU02 Android 14. La versión instalada de referencia es mando-autonomo.apk, SHA-256 2B9C2E6984DF82546697B575300DC369EE93E4621D130B84D74DE52B58B2102A. El nombre Lumen TV es posterior a esa instalación.

La recuperación de Google TV está documentada en INICIO-REAL-G08-AU02.md y en tools/restore-c6k-home.ps1. Leer el estado real antes de escribir. La configuración de paquetes de este equipo requiere una recuperación específica y aprobación del propietario.

Los botones globales de Ajustes y YouTube tienen un ensayo local en tools/c6k-mando.ps1. Requiere iniciarse de nuevo después de reiniciar. El filtro de accesibilidad afectó al volumen mantenido y permanece desactivado.

Exportación de ajustes, búsqueda en biblioteca, alias HDMI y la validación completa de música/notificaciones siguen pendientes. Compilar no sustituye probar el mando y el firmware físico.

## Continuidad y privacidad

Se conservan el código de producción, pruebas, herramientas, recursos y documentos. Los registros privados del televisor, APK, capturas, cachés, SDK/JDK y la descompilación de Monet permanecen fuera de Git. La limpieza de noviembre retiró evidencia binaria histórica y algunos enlaces antiguos ya no existen.

Los APK aprobados y de reversión se conservan localmente en artifacts/conservado. Cualquier instalación en la TCL, activación de servicios o cambio del sistema requiere autorización específica. Las normas visuales están en DESIGN-SYSTEM.md.
