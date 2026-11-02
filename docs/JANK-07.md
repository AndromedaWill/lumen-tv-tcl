# Jank 07 · caracterización sin cambiar la TCL

Fecha de trabajo: 1 de noviembre de 2026 (America/Lima). No se ejecutó ningún comando contra el televisor real. Las medidas corresponden al AVD `TCL_Home_1080`, serial explícito `emulator-5554`, API34 x86, 1920×1080/densidad320, Skia/OpenGL mediante gfxstream y el traductor OpenGL del emulador sobre AMD Radeon. No son cifras del MediaTek de la TCL.

## Problema y método

En la entrega anterior se observó más jank en C49 que en EC8011. No se borra ese resultado ni se presenta como resuelto por un cambio de color. Se repitió el protocolo con orden **A–B–B–A**, para reducir la confusión entre versión y orden temporal: mismo catálogo de fixtures, foto fija Día, preferencias preservadas, arranque de proceso, espera de cuatro segundos y doce ciclos Down/Atrás separados por 900ms. Sin compilaciones, grabaciones ni otra automatización de UI durante las medidas.

EC8011 es nuestra revisión anterior instalada en la TCL; no es el proyecto base original. 10DB es el último checkpoint previo a los Ajustes jerárquicos. Todas las instalaciones de esta investigación se dirigieron al emulador comprobado con `ro.kernel.qemu=1`. No se cambió HOME ni se concedió acceso a servicios.

| Orden | APK | Jank | p50 / p95 / p99, ms | PSS, KiB |
|---|---|---:|---|---:|
| A1 | EC8011 | 14.20% | 25 / 46 / 53 | 43,733 |
| B1 | 10DB | 16.73% | 26 / 46 / 53 | 41,847 |
| B2 | 10DB | 15.68% | 25 / 48 / 57 | 41,982 |
| A2 | EC8011 | 15.90% | 24 / 46 / 53 | 43,592 |

Frente a la sesión anterior, **también empeora EC8011**, de aproximadamente 4.5–5% a 14–16%. Hay una influencia importante de la sesión/pipeline del emulador; la diferencia pequeña entre APKs no queda causalmente aislada. Dos muestras por versión no permiten descartar una regresión propia ni dar una estimación robusta de su tamaño. No se mezclan estas sesiones para elegir solo las cifras favorables.

Datos: `artifacts/stage-07/baseline-abba/`, incluyendo APK SHA-256, cuadros brutos, memoria, captura y etapas.

## Dónde se está consumiendo el tiempo

Se analizaron los timestamps válidos del anillo `framestats` de cada ventana: 116–120 fotogramas en el tramo conservado, no todos los 514–528 del histograma agregado. Solo Flags=0; diferencias no positivas o mayores de un segundo se excluyen de percentiles por etapa, conservando el archivo original. Los percentiles de etapas usan rango más próximo, no los buckets de `gfxinfo`.

- Trabajo de UI hasta SyncQueued: p95 **1.15–4.34ms** en esta ronda.
- Registro de dibujo: p95 **0.21–0.72ms**.
- Emisión de comandos: p95 **2.43–5.74ms**.
- Tramo SwapBuffers → SwapBuffersCompleted: p95 **27.25–29.86ms**.
- Los contadores marcan 70–79 incidencias de emisión lenta frente a 15–22 de hilo UI lento.

El tiempo dominante observado está en la espera/presentación de la tubería gráfica, no en una decodificación de fondos o consulta de red del key handler. Estos timestamps son esperas/transcurridos de CPU en un pipeline asíncrono: **no son tiempo puro de GPU ni latencia física del mando**. El traductor de gráficos, sincronización del host y carga de la sesión pueden influir. No se asigna sin una traza de hardware una causa única al driver o a la app.

Android describe cómo las esperas de GPU y la emisión de comandos pueden bloquear el pipeline: [Inspect rendering speed](https://developer.android.com/topic/performance/rendering/inspect-gpu-rendering). El p99 GPU de 4950ms que aparece en algunos histogramas no se interpreta como un bloqueo real de 4.95s; los tiempos de fotograma y el anillo no respaldan esa lectura.

## Hipótesis comprobada: capas de las primeras tarjetas

Un cambio de EC8011 a la revisión actual limitó las capas persistentes de tarjetas a efectos avanzados. Se construyó **2CCB3970E85D**, idéntico a 10DB excepto por recuperar esas capas también en modo 2D animado. Es una ablación de una condición, sin cambios visuales. Se probó de nuevo A–B–B–A:

| APK | Dos muestras de jank | Dos p95, ms | Dos PSS, KiB |
|---|---|---|---|
| 10DB, control | 15.13% / 13.37% | 46 / 42 | 41,783 / 41,808 |
| 2CCB, capas recuperadas | 14.23% / 13.88% | 42 / 44 | 41,798 / 41,857 |

No hay mejora clara fuera de la variación observada. Las cachés GPU también variaron en rangos solapados, por lo que no se les atribuye una diferencia de memoria demostrada. **La condición experimental se revirtió**; el candidato conserva la política ligera de 10DB. No se añaden capas, precargas o efectos por intuición.

Datos y APK de diagnóstico: `artifacts/stage-07/layers-abba/` y `layer-probe-2ccb3970e85d.apk`. Ese APK experimental no se recomienda para instalar en la TCL.

## Optimización aplicada a los Ajustes nuevos

En el primer prototipo jerárquico, cada push/pop volvía a ejecutar Config.load, que consulta recursos y puede leer/parsing el archivo del tema. Se corrigió: una carga al crear la Activity y otra solo al restablecer el tema; columnas, filas y opciones de movimiento se actualizan en el estado de esa sesión al guardar. Navegar o devolver el foco no vuelve a consultar el archivo. No es una supuesta solución al jank del catálogo, sino eliminar E/S y asignaciones innecesarias en el nuevo flujo.

Solo se construye la página visible. La pila guarda enums y claves de foco, no árboles de Views ocultos. Sin servicio nuevo, fuente, imagen grande, blur vivo o dependencia adicional.

## Reproducción y siguiente decisión

Herramientas: `tools/bench-grid-transition.py` y `tools/compare-navigation-emulator.py`. Ejemplo, con una carpeta de salida nueva:

```text
python tools/compare-navigation-emulator.py --serial emulator-5554 --baseline artifacts/lumen-review/tcl-home-10db36445cff.apk --candidate artifacts/stage-07/candidate-a50262855ff1.apk --output artifacts/comparison-new
```

La herramienta rechaza un serial físico y restaura el candidato al terminar; conserva preferencias, no cambia HOME. No ejecutar compilaciones ni capturas de vídeo al mismo tiempo. El histograma conserva todos sus fotogramas; las etapas derivadas describen solo el anillo final. El primer Down se registra separado, sin convertirlo en una medida de botón remoto.

La comprobación final contra el candidato jerárquico, y las medidas propias de sus Ajustes, se añaden en [ETAPA-07.md](ETAPA-07.md). **Estado del problema: caracterizado, no cerrado en hardware.** Antes de declarar fluidez final, repetir la ruta y tomar una traza limitada del paquete en la TCL con autorización. No bajar resolución/densidad del televisor, tocar escalas globales ni desactivar servicios para mejorar estos números.


## Ajustes: evitar relayout de la descripción lateral

Se aisló un segundo coste del prototipo jerárquico: la descripción lateral tenía altura wrap_content y cambiar de opción podía pedir otra medición de la página. Se reserva su espacio disponible y se alinea el texto arriba, conservando la posición del contenido. El cambio solo afecta a `SettingsPageView`; no añade cachés de imágenes, servicios o capas GPU.

A–B–B–A entre A502 (altura dinámica) y 39264 (espacio reservado), veinte Down/Up por muestra, cuarenta fotogramas registrados. Mismo aspecto y mismo recorrido de categorías:

| Orden | Descripción | p50 / p95 / p99, ms | UI p95, ms | Layout p95, ms |
|---|---|---|---:|---:|
| A1 | Dinámica | 21 / 44 / 48 | 2.083 | 0.928 |
| B1 | Reservada | 18 / 19 / 34 | 1.575 | 0.856 |
| B2 | Reservada | 18 / 18 / 19 | 1.041 | 0.457 |
| A2 | Dinámica | 17 / 22 / 34 | 2.287 | 1.339 |

La ronda respalda reducir trabajo de UI/layout. Son dos muestras por variante y no se promete que la misma mejora se reproduzca en la TCL. PSS de ambas variantes se solapa; no se declara una reducción de memoria. Datos completos: `artifacts/stage-07/description-abba/`. Se conserva esta corrección de layout y se comprueba visualmente que los textos y controles no se desplazan.

El contador moderno de jank marca 100% en esos cuarenta cambios aislados **en ambas variantes**. La referencia de Ajustes planos 10DB también da 100% con ese contador, mientras el legado y los percentiles son diferentes. Se conservan todos: no se sustituye silenciosamente uno por otro ni se interpreta ese porcentaje aislado como FPS. En la ronda 10DB/A502, una muestra de A502 llegó a p99=300ms; sigue visible en `settings-abba/`, no se elimina como si nunca hubiera ocurrido. El protocolo no permite determinar si una anomalía del pipeline o trabajo de la app explica cada outlier. Para la decisión se contrastan etapas, distribución, estado visual y finalmente hardware.

La mejora medida en Ajustes **no cierra el caso del dock/catálogo**. Esa ruta requiere la validación física posterior autorizada, con traza si continúa la pausa que percibe el usuario.
