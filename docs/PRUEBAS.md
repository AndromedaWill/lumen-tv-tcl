# Verificación de desarrollo — 31/10/2026

## Alcance y entorno

Solo emuladores Android TV API 34 x86, WHPX, cuatro CPU, 2 GB de RAM y GPU host con Vulkan desactivado. Emulator 37.2.12 sobre Windows. Ningún acceso al televisor TCL. El AVD no reproduce el firmware TCL, HDMI físico, CEC, DRM comercial ni la resolución del panel 4K.

El original `046da15` se compiló antes de modificar producción: assembleRelease y 124 pruebas JVM aprobadas. Lint original: tres errores por BOM literal y 70 advertencias. Corrección aislada en `b44c4f6c`, sin suprimir reglas. El APK original ya excedía el gate heredado de 400 KiB; no se presenta CI como aprobada.

## Resultado de compilación

Revisión de material Lumen: 124 pruebas JVM, cero fallos/errores; lint sin errores, 80 advertencias y 125 informaciones (mayormente traducciones, RTL y mantenimiento heredado). No hay dependencias nuevas. El APK de prueba usa firma de desarrollo y paquete `pe.tclhome.prototype`.

## Pruebas de mando y estado

`artifacts/layout-smoke-1080/results.json`: 19 escenarios aprobados en 1920×1080/density 320. Se verifican límite izquierdo del dock; nombre accesible; apertura de Archivos (APK fixture independiente); Atrás al launcher; persistencia del foco tras pérdida del proceso; bajar a las apps; Atrás al dock; reloj/barra superior; apertura del centro; navegación horizontal/vertical y límites de la cuadrícula; cierre devolviendo foco; apertura de ajustes oficiales de Android TV.

El script espera hasta 10 s por un cambio de actividad y registra fallos: es una comprobación funcional, no un ensayo de latencia. Las primeras corridas con espera fija de 0,8 s fallaban durante compilaciones concurrentes; se corrigió el sincronismo del ensayo y no se cuentan como aprobación. Las mediciones de rendimiento se ejecutan sin compilación concurrente.

Las capturas son PNG del framebuffer obtenidos mediante screencap/pull, sin retoques. Los iconos de Archivos/Cine/Colección/En directo/Fotos/Juegos/Música/Radio son ocho APK de prueba instalados de verdad; no forman parte del launcher ni representan apps comerciales. YouTube y las aplicaciones del AVD son detectadas por PackageManager.

## Límites y pruebas pendientes

No certificada para uso diario en TCL. Pendiente identificar firmware peruano mediante ADB de solo lectura y aprobar la prueba física. Por aprobar en hardware: teclas del mando TCL, HDMI/CEC/eARC/Game Bar, servicios nativos, reinicios y uso prolongado. Reordenar/ocultar reutiliza la implementación original, pero requiere ensayo completo de persistencia; importar cinco imágenes, franjas horarias y sus errores aún no están implementados. No se probó vídeo aéreo, almacenamiento lleno, rotación/recreación completa ni volumen experimental.

TvInputManager del AVD no presenta entradas HDMI: el selector no inventa puertos y muestra indisponibilidad cuando no hay fuentes reales. Accesibilidad está apagada; no existe sustitución global de volumen. Inicio predeterminado conserva el launcher del AVD.

## Rendimiento

Datos brutos y protocolos: `tools/bench-emulator.py`, `artifacts/*/summary.json`, gfxinfo, meminfo, cpuinfo y top. `am start TotalTime` no equivale a primer frame interactivo. No se convierte RAM libre en indicador de rapidez. Una corrida inicial SwiftShader produjo ANR del baseline; se descartó para comparación. Los histogramas GPU del emulador contienen valores centinela y no certifican rendimiento de GPU real.

### Comparación inicial con fotografía de resolución completa

Mismo AVD 1080p y recorrido de 48 teclas, cinco arranques por estado, sin compilar en paralelo. Original: mediana fría 1061 ms, HOT 93 ms, PSS 45895 KiB, 931 frames, 35 janky (3,76 %), percentiles CPU/frame 23/32/38 ms (p50/p90/p95). Prototipo con foto completa: 682 ms, HOT 97 ms, PSS 68219 KiB, 815 frames, 258 janky (31,66 %), 34/69/81 ms. CPU en reposo por top: original 0–1 %, prototipo 0 %. Los valores legacy de jank son distintos y se conservan en bruto: no son métricas intercambiables.

Esta corrida detecta una regresión de renderizado y memoria; no permite afirmar que el prototipo sea más fluido. Por ello se redujo la foto a la mitad por eje y se eliminó el segundo pase de oscurecimiento a pantalla completa. La comparación inicial está en comparison-original-1080 y comparison-lumen-1080; se conserva como antecedente, no se borra una medición desfavorable. El fondo no puede considerarse validado para uso diario sin pruebas en hardware y mayor optimización si persiste el jank.

### Reducción de foto y de overdraw

`comparison-optimized-1080`: mediana fría 1331 ms, HOT 95 ms; PSS 50771 KiB; 868 frames, jank 15,78 %, p50/p90/p95 25/57/69 ms. Frente a la foto completa, PSS baja 25,6 % y el porcentaje de jank cae aproximadamente a la mitad. Sigue por encima del original: no se presenta como rendimiento final aceptado. El paso siguiente cachea el material del dock para reemplazar clip, muestreo y gradientes por un solo drawBitmap durante movimiento; también elimina una segunda capa de reflejo redundante.

### Material cacheado del dock

`comparison-cached-1080`, misma secuencia sin compilación concurrente: mediana fría 1414 ms, HOT 93 ms, PSS 54314 KiB, 974 frames, 97 janky (9,96 %), p50/p90/p95 26/40/48 ms. Reduce el jank frente a la foto completa en aproximadamente 68,5 %, con un coste de memoria frente al paso anterior que se acepta por reducir el trabajo por frame. Sigue peor que el original en jank y arranque frío de esta corrida; no se promete 60 fps ni superioridad general. CPU en reposo y otros detalles permanecen en los logs top/cpuinfo. Son corridas individuales de desarrollo, no una estimación estadística del hardware TCL.

Después de medir se oscureció únicamente el tinte del dock para acercarlo a la referencia: mismo trazado/cache/dimensiones. El APK final se recompila y valida, sin atribuirle una medición nueva por ese ajuste cromático.

## Revisión de proporciones y transparencia de Lumen

Tras la comparación visual se detectó que la primera caché copiaba una foto opaca debajo del tinte. Se reemplazó por material ARGB translúcido independiente, con medidas trasladadas de TILES_FOR[6] y glass-bar. Se incorpora la transición al fondo preparado/difuminado al bajar y se elimina el título que el usuario rechazó. Las cifras anteriores corresponden a prototipos previos y no certifican esta revisión; se conservan para trazabilidad. Geometría y recursos en ADAPTACION-LUMEN.md.

## Navegación en ambas resoluciones

final-smoke-1080: 21/21 escenarios aprobados. final-smoke-720: 21/21 aprobados con 1280×720/density 213 y el modo 2D/capas visibles. Se incluyen el acceso al catálogo completo y el retorno del foco a su botón. Capturas home, library, catalog y settings en sus respectivas carpetas; PNG sin editar. El Inicio predeterminado se consultó con audit-home.ps1 y sigue siendo com.google.android.tvlauncher/.MainActivity. No se ensayó un cambio de Home ni su reversión porque no está autorizado todavía.

La revisión geométrica con velo y foco 3D arrojó 17,59 % de jank, PSS 57859 KiB, p50/p90/p95 30/48/61 ms, cold 1366 ms y HOT 198 ms (comparison-lumen-reference-1080). Se conserva ese resultado: el velo añade una transición que la prueba anterior no tenía. El siguiente candidato limita el parallax a advanced_effects y conserva capas visibles con liberación en pause/detach; necesita su propia corrida, no reutilizar el porcentaje anterior como si fuese actual.
