# TCL Home: desarrollo del fork

Leer `C:\Users\milmil\.codex\RTK.md`. Este proyecto es un fork de desarrollo, no una sesión de debloat.

- No tocar la TV real, cambiar Home predeterminado ni habilitar accesibilidad sin aprobación específica.
- No ejecutar build-and-install, disable, restore, theme ni herramientas de configuración del fabricante durante desarrollo.
- Desarrollo y ADB de escritura solamente contra un serial `emulator-*` verificado. Nunca seleccionar el primer dispositivo automáticamente.
- Preservar servicios nativos, SystemUI, red, DRM, actualizaciones y paquetes. No root ni menús de fábrica.
- Java 17, SDK 34, Gradle wrapper verificado; compilar, pruebas JVM y lint antes de entregar.
- Separar cada nueva responsabilidad; reutilizar biblioteca, dock y navegación existentes. Sin dependencias nuevas sin necesidad concreta.
- Capturas reales del emulador y métricas brutas en artifacts (ignorado); nunca presentar maquetas como ejecución.
- La aprobación visual precede al siguiente rediseño mayor. El volumen experimental permanece separado y apagado.
- Mantener licencias MIT/OFL y referencias de los ports. No recursos propietarios de Apple nuevos.
