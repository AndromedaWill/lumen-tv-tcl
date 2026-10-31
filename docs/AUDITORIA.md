# Auditoría inicial — TCL Home

Fecha: 30 de octubre de 2026, America/Lima. Alcance: código, documentación e historial; ninguna conexión ni modificación de la TV física. Las métricas y capturas finales se documentan en PRUEBAS.md.

## Procedencia comprobada

La carpeta entregada contenía un repositorio vacío, sin commits ni remotos. Se recuperó `origin` de https://github.com/nickoliverio40/apple-tv-launcher y `cleanyourtv` de https://github.com/Gathin23/cleanyourtv. Rama de desarrollo: `codex/tcl-home-prototype`.

| Referencia congelada | Revisión | Versión APK |
|---|---|---|
| Apple TV Launcher | 046da15 | 1.2 / 12 |
| CleanYourTV | fe9ad320 | 3.2 / 11 |
| Ancestro común | 687b5ee2a3f3151f5d5e09d82b693d60fa626bdb | — |

Desde el ancestro: 17 commits exclusivos del fork y 3.592 de upstream. Los versionCode pertenecen a líneas distintas: 12 no significa que el fork incorpore las correcciones de 3.2. Se revisaron README, CONTRIBUTING, SECURITY, manifiestos, Gradle, configuración, ciclo de vida, carga de apps, entradas, servicios, vídeo y documentación TCL. El fork no traía AGENTS.md; se leyó el de upstream completo. Su distinción entre desarrollo y limpieza autoriza cambios de código en un fork propio; no se aplican instrucciones de debloat como procedimiento de desarrollo.

## Arquitectura y funciones existentes

Java con Views XML, RecyclerView 1.3.2 y DynamicAnimation 1.0.0; sin Compose ni framework adicional. Gradle 8.9 (SHA-256 fijado), AGP 8.5.2, Java 17, compile/target 34, min 26. Release usa R8 y reducción de recursos; sin keystore propio cae a firma debug (no apropiado como estrategia definitiva de distribución).

`MainActivity` concentra navegación, popups, red, entradas, reloj y coordinación: ya es una clase muy grande. `AppAdapter` descubre aplicaciones, representa tiles y carga iconos; `Order`, `DockSlots`, `GridNavigation`, `LibraryApps` y `AppLibraryDialog` resuelven orden, biblioteca y foco. `Config` mezcla recursos, tema JSON y preferencias. `TvFocusMotion`, `LiquidGlassDrawable` y `ShineView` son extensiones visuales del fork.

Ya existen seis columnas, dock desplazable, biblioteca, reordenamiento D-pad, ocultación, nombres/banners configurables, accesos a ajustes, foco por clave en memoria, modo de movimiento reducido, fondos fijos/rotativos y módulos opcionales. No duplicar estas funciones. La retención por campos static no garantiza restauración tras muerte del proceso: añadir persistencia acotada. La carga de apps y sus recursos necesita medición antes de decidir su extracción a un executor.

`WallpaperPlayer` y `WallpaperSize` limitan decodificación y rotan imágenes; no constituyen un importador de cinco franjas horarias. `SkyView` cambia colores según hora, no sustituye imágenes por horario. `AerialBackdrop` usa MediaPlayer/SurfaceView, prepara asíncronamente y libera el decoder en onPause/surfaceDestroyed. Esto es evidencia de diseño, no prueba de funcionamiento de un vídeo en TCL. Su ciclo usa una cubierta negra entre clips; no confundirlo con crossfade de imágenes. `AerialDownloads` añade red y descargas explícitas, ausentes en upstream. No descargar ni distribuir vídeos de Apple en esta entrega.

## Permisos y límites de seguridad

El fork añade INTERNET para aéreos. Conserva ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE y ACCESS_FINE_LOCATION (SSID; no se necesita para fondos por hora). No conceder ubicación automáticamente. El proveedor ThemeProvider está exportado pero protegido por WRITE_SECURE_SETTINGS. Auditar cualquier futura ruta de archivo importado y usar nombres internos propios. Servicios de accesibilidad HomeKeyService, InputKeyService y SleepService se declaran, pero declararlos no equivale a activarlos. El prototipo no debe solicitar su activación. No hay permiso SYSTEM_ALERT_WINDOW, control privilegiado de audio ni capacidad de reemplazar SystemUI.

Los instaladores heredados tienen efectos sobre Home, accesibilidad, permisos y launcher original; no usarlos para compilar. Los scripts de limpieza antiguos no tienen todas las protecciones recientes de upstream. Permanecen como historia, fuera del flujo autorizado. El README heredado contiene instrucciones destructivas respecto a paquetes: no son el procedimiento de este fork.

## Ports seleccionados y descartados

| Área / evidencia de upstream | Decisión | Dependencias y validación |
|---|---|---|
| SystemSettings, 020c7273, 50e0269e, 9d0f88f9 | Adaptar resolución segura de ajustes nativos | Consultar activities exportadas/habilitadas; capturar SecurityException, no asumir firmware |
| InputNames, c492a229 | Candidato fase HDMI | Normaliza etiquetas Philips; conservar labels TCL; tests de nombres vacíos y `null` |
| InputSources, cb8ebae2 y 22e5884f | Candidato fase HDMI | Acciones/componentes configurables; no copiar rutas Xiaomi en TCL |
| LibraryLoads, 8a569b8b | Candidato al extraer biblioteca | Tokens de generación para resultados obsoletos; probar cambios de paquetes durante carga |
| MemoryTrimPolicy, b3223648 | Candidato fase rendimiento | Liberar fondo al recibir trim; reconstruir al volver, evitar imagen reciclada en View |
| TileRefresh / Order / DockSlots | Revisar por unidad, conservando movimiento del fork | No copiar AppAdapter entero; difiere geometría, foco y artwork |
| HomeKeyService / RemoteKeys / VolumeKeys, 45be1e14 | NO activar ni portar interceptación ahora | Workaround de volumen no demuestra compatibilidad G08; requiere mando físico y rutas de audio |
| GuardKeep, ServiceRepair, autoarranque, deshabilitación | Fuera del prototipo | Cambian estado del sistema/servicios y no son necesarios para la pantalla |
| Localización es-419 | Candidato tras estabilizar textos | Conservar strings nuevos del fork; no sobreescribir recursos completos |
| Festividades, tarjetas, nuevas decoraciones | Posponer | No aportan a esta primera entrega minimalista |

No hacer merge completo: upstream no contiene AerialBackdrop, TvFocusMotion, LiquidGlassDrawable, NavSound ni las demás extensiones del fork. Una actualización indiscriminada perdería trabajo visual o introduciría servicios no solicitados.

## Criterios de rendimiento y diseño

Foco por tamaño, elevación y contraste, espacio estable, etiquetas legibles a distancia y transiciones interrumpibles; sin desenfoque en tiempo real. Fondo original procedural estático para esta revisión, sin activos propietarios. Reutilizar tipografías Inter bajo OFL.

La resolución del panel no determina la de la UI: [Android documenta esta separación](https://developer.android.com/training/tv/playback/memory). Dimensionar bitmaps por superficie renderizada, no por 4K del marketing. Medir PSS total, CPU, arranque y gfxinfo con mismo AVD y recorrido; no inferir rendimiento de RAM libre ni extrapolar emulador al SoC TCL. Referencia de interacción: [Apple, focus and selection](https://developer.apple.com/design/human-interface-guidelines/focus-and-selection).

Pendiente real: inventario peruano, reproducción protegida, CEC/eARC, mando y aceleración de volumen, vídeo de usuario, estrés prolongado y aprobación visual. Ninguno se marca aprobado por inspección de código.
