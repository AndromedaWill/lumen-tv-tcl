# Entrega 06 · refinamiento coherente para revisión visual

Fecha: 01/11/2026. Desarrollo local en `codex/tcl-home-prototype`. La TCL sigue con EC8011; no se ha ejecutado ningún comando dirigido al televisor en esta revisión ni cambiado HOME, paquetes, accesibilidad o permisos de notificaciones.

## Resultado visual

Se mantienen las distribuciones existentes y se comparte un material de cristal acromático: algo más de cobertura que la variante más transparente, sin rellenos marrones, azul oscuro o azul gris. Los colores de la fotografía pueden verse por transparencia; no se desatura la imagen principal. `DESIGN-SYSTEM.md` y `design_tokens.xml` son la referencia para las funciones siguientes.

- Calendario a 80dp del borde superior: separación del dock de 42px a 1080p y 25px a 720p, incluso en el mes de seis filas comprobado.
- Centro de control compacto, sin título redundante y con menos separaciones. «Ajustes nativos» y «HDMI y TV» caben en sus bounds; padding de fuente y alineación corregidos sin reducir el texto.
- Notificaciones resumidas sin caja exterior: el estado apagado muestra solo «Notificaciones desactivadas», con el mismo material. El acceso continúa apagado.
- Personalizar, Fondos, opciones, filas de entradas, selectores, foco y diálogos propios comparten fuente, borde y estados. Los diálogos conservan toda la pantalla base visible y estable, con el oscurecimiento de ventana compartido. Se retiró el intento de ocultar sus controles tras la revisión del usuario. Atrás devuelve el foco sin reconstruir la pantalla.
- Biblioteca con el mismo arte que Inicio, sin duplicar las tarjetas del catálogo que había debajo y sin el texto «En Inicio» redundante con ✓. Restauración de alpha/foco antes de devolver el control.

El contraste de los subtítulos sobre un JPEG blanco de prueba fue de 5.59:1–6.15:1 en los puntos medidos. No certifica cualquier fondo, escala de letra o lector de pantalla. Se conservan el diseño nativo del selector de documentos, teclado, permisos y ajustes del fabricante.

## Evidencia reproducible

Los artefactos están ignorados por Git; no son maquetas ni imágenes retocadas.

| Evidencia | Ubicación |
|---|---|
| Comparador de cinco pantallas y estados de foco, 720p/1080p | `artifacts/design-system/review.html` |
| Antes/después de diálogos secundarios y comprobación a 720p | `artifacts/design-system/modals.html` |
| 22 capturas de pantallas principales, APK C49 | `artifacts/design-system/after/{1080,720}/` |
| Diálogos y restauración, APK final D5F7 | `artifacts/tcl-review-06/modal-context/{1080,720}/` |
| Geometría / textos / contraste blanco | `artifacts/design-system/{calendar-fit,text-fit,contrast-white}.json` |
| Compilación, firma y manifiesto del APK final | `artifacts/tcl-review-06/{build-context-preserved.log,signature-final.txt,manifest-audit.json,checks.json}` |
| Pruebas de galería / vídeo / memoria | `artifacts/tcl-review-06/qa/` y `video-verified/` |

Las pantallas principales C49 conservan el mismo material/geometría en D5F7; el único cambio visual restante frente a C49 neutraliza el borde de la vista previa; se eliminó por completo la ocultación de controles detrás de los diálogos secundarios. No se reetiquetan capturas anteriores como pertenecientes a otro APK. Los iconos Archivos/Cine/Colección del emulador son apps fixture.

El build final ejecutó **148 pruebas JVM, 0 fallos/errores**, `lintRelease` sin errores ni fatales (**123 advertencias conservadas**) y `assembleRelease`. El recorrido final de **36 escenarios D-pad pasó íntegro también en D5F7** (`smoke-final-context/`); C49 comprobó las 22 capturas y el ciclo de vídeo. La conservación de contexto de diálogos D5F7 se verifica de forma específica a ambas resoluciones, incluyendo cierre y restauración. No se concede acceso a notificaciones para adelantar pruebas.

Para reproducir en Windows: `tools/build-prototype.ps1` usa Java 17, SDK 34 y el wrapper fijado. Las capturas usan `tools/capture-design-emulator.py --serial emulator-5554 --height 1080 --output ...` (y 720); el helper rechaza seriales físicos y comprueba `ro.kernel.qemu=1`. Solo ese emulador recibió instalaciones y cambios temporales de tamaño/densidad; se restauran al terminar. No ejecutar estas pruebas contra el transporte de la TCL.

## Rendimiento: resultado y límites

Comparación controlada **EC8011 frente a C49**, en la misma ruta que D5F7; el cambio restante solo afecta al borde de la vista previa de Fondos. Mismo emulador API 34 x86, 1920×1080/densidad 320, catálogo de fixtures, foto fija Día y doce recorridos dock → abajo → Atrás con esperas de 900ms. La región fotográfica central coincide exactamente en ambos pares. Sin compilación ni grabación simultáneas. Métricas de histograma de `gfxinfo`, no latencia real del mando.

| Par | APK | Jank | p50 / p95 / p99 (ms) | PSS (KiB) |
|---|---|---:|---|---:|
| 1 | EC8011 | 5.03% | 18 / 25 / 31 | 42,817 |
| 1 | C49 | 6.64% | 18 / 29 / 38 | 41,014 |
| 2 | EC8011 | 4.48% | 18 / 20 / 23 | 42,792 |
| 2 | C49 | 6.51% | 18 / 31 / 36 | 41,233 |

El PSS baja aproximadamente 1.5–1.8MiB, pero los fotogramas son algo peores en estos dos pares. **No se declara una mejora de fluidez ni se recomienda aún como Inicio definitivo.** En la primera bajada p95 fue 34→31ms en el primer par y 25→26ms en el segundo: no demuestra eliminada la pausa que observó el usuario en la TCL. P1: repetir el recorrido con medición controlada en hardware autorizado antes de ampliar funciones/animaciones. Conservar esta regresión como límite abierto, no corregir los números mediante más muestras selectivas.

Datos brutos: `artifacts/tcl-review-06/benchmark-validated/`. Una corrida en `benchmark-final/` fue invalidada por solaparse con la restauración de UI y lleva `INVALID.txt`; no se usa en la tabla. La comparación anterior C354 también se conserva, pero no reemplaza estas cifras. EC8011 es nuestra revisión anterior, **no** el fork original; no existe una comparación física equivalente con el proyecto base original.

La foto principal se decodifica realmente antes de dibujar el bitmap de destino hasta Full HD. `decodeMs` rodea `BitmapFactory.decodeStream`; el blur y la preparación se miden por separado, en el worker. Los tres bitmaps CPU actuales suman 8,409,600 bytes a 1080p; la variante neutral cuesta 57,600 bytes adicionales, compartidos. Eso no incluye memoria temporal, GPU ni PSS total. No se hace blur continuo, no hay SDK o dependencia nueva y los registros se emiten por carga/apertura, nunca por fotograma.

## Vídeo

La prueba final C49 con H264 propio confirma actividad fixture reanudada, cliente MediaPlayer del launcher ausente durante la pausa y presente al regresar. La cubierta pertenece al mismo vídeo; no se sustituye por una imagen de la colección. En `video-verified/transition.mp4` no se detectan intervalos casi negros con el umbral 98% de píxeles bajo 8% de luminancia durante al menos 10ms. `RESUMED` no implica que la otra app haya dibujado todavía: su ventana inicial puede conservar el snapshot del launcher.

Espera de cubierta antes del lanzamiento: 158ms en esa grabación; cinco ciclos sin grabación: 113/55/47/72/80ms (mediana 72ms). Se conserva un valor previo bajo carga de 310ms. El plazo de copia programado de 96ms no garantiza latencia de tiempo real: se añade el fotograma de dibujo y el despacho del hilo UI. Los cinco ciclos de memoria de C354 terminaron estabilizados en aproximadamente 76.2MiB PSS; no son una medición del APK final ni de la TCL. No se atribuye la variación al grabador como causa demostrada.

Pendiente P1: repetir con los vídeos aéreos reales y el decodificador de la TCL; las transiciones/ventanas iniciales o DRM de otras apps no se pueden sustituir desde un launcher ordinario.

## APK conservado y siguiente puerta

- Archivo: `artifacts/tcl-review-06/tcl-home-d5f7877c811f.apk`.
- SHA-256: `d5f7877c811ff91ba55c9d4482626c6cd44a4a983f273fe502bdb5e569e956f5`.
- Tamaño: 6,441,836 bytes. Paquete `pe.tclhome.prototype`, versión 12/1.2, separado de TCL/Google.
- minSdk 26 / targetSdk 34; sin bibliotecas nativas `.so`, por lo que no exige arm64 ni excluye armeabi-v7a. Validación física de esta revisión pendiente.
- Firma APK v2 válida para API 34, mismo certificado de desarrollo `b80940487f3da13532040f22ed5fe2b9a6c370377ab430110a1489d3fb7617f6`. No es una firma de distribución pública.
- Cuatro permisos heredados (Internet/red/Wi-Fi/ubicación); sin permiso amplio de almacenamiento, overlay o lectura de cuentas añadido. Cinco actividades, seis servicios protegidos declarados, cero receptores estáticos y proveedor de temas protegido. Declarar un servicio no lo activa: HOME, accesibilidad y notificaciones requieren acciones manuales independientes, que no se ejecutan aquí.

La sustitución funcional de Google TV sigue siendo el objetivo final. Esta entrega se presenta para aprobación visual. Una actualización física requiere nueva autorización concreta y se abre primero como app normal; no se combina con cambiar HOME, HDMI, volumen o limpiar paquetes. Conservar EC8011 y sus preferencias como referencia. Google TV permanece instalado/habilitado para regresar con Home y recuperar el entorno conocido.

Pendientes: proveedor de archivos de TCL y mando dentro de ese proveedor (el del emulador precisó Tab/toque sobre el vídeo propio), nitidez de las apps reales, Google Home desde el nuevo APK, métricas físicas, notificaciones activas y pruebas de reinicio/rol HOME. No se afirma rendimiento del visor activo ni sustitución global del volumen.


Durante la prueba local prolongada el proceso del AVD se cerró al pasar a 720p; el log del renderizador contenía errores de color buffer. Se reinició el mismo AVD, sin borrar sus datos, para repetir la validación pendiente. No se atribuye ese cierre al launcher ni se usa como evidencia de estabilidad de la TCL. La incidencia está en `modals-final.log`; la repetición completada en `modal-context.log` y `emulator-recovery*.log`.

La comprobación de contexto mide los píxeles del título de la pantalla base antes/durante el modal: conserva una luminancia relativa de 0.351 a 720p y 1080p, compatible con el dim del 65%, en vez de desaparecer. `modal-context/context-visibility.json` guarda el cálculo y las capturas originales.


### Continuación tras el estudio específico de Lumen

Se conserva D5F7 como checkpoint. La revisión posterior 10DB36445CFF solo unifica tintas secundarias/glyphs con los neutros existentes: [LUMEN-COMPARATIVA.md](LUMEN-COMPARATIVA.md) documenta fuentes, cambios, pruebas y su APK conservado. El estudio no añadió nuevas funciones ni efectos. La regla de consulta previa sobre colores importantes está en DESIGN-SYSTEM.md y AGENTS.md.
