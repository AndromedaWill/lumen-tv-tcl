# Primera prueba visual en TCL C6K

## Estado y límite de esta entrega

Preparación local completada el 31 de octubre de 2026 (Lima). **Instalación y apertura en la TCL realizadas tras las cuatro autorizaciones explícitas del usuario**. Este documento no presenta resultados del emulador como mediciones de la TCL. La prueba inicial es una aplicación normal: no se cambia Inicio, accesibilidad, salvapantallas, HDMI, audio ni paquetes del fabricante.

La TCL diagnosticada usa Android 14/API 34, aplicaciones ARM de 32 bits (`armeabi-v7a`), firmware G08/AU02 y una interfaz ya configurada a 1920×1080 sobre panel 3840×2160. No se ha cambiado esa configuración. Usuario Android: 0.

## Candidato congelado

- APK: `artifacts/first-tcl/launcher-first-tcl-b859afc8a5b3.apk` (ignorado por Git).
- SHA-256: `b859afc8a5b323ee7cb16aeb921314d00bed3c04643b4a3343d2a22ed3bc37bd`.
- Paquete: `pe.tclhome.prototype`; etiqueta: `TCL Home · Prueba`; actividad: `com.gathin.tvhome.MainActivity`.
- Java 17, compile/target SDK 34, mínimo 26. DEX y recursos sin bibliotecas nativas: no exige ARM de 64 bits y admite el entorno ARM de 32 bits del televisor. La ejecución física sigue pendiente.
- Firma APK v2 válida, comprobada con `apksigner verify --min-sdk-version 34 --max-sdk-version 34 --verbose --print-certs`. Certificado local Android Debug/RSA 2048: SHA-256 `b80940487f3da13532040f22ed5fe2b9a6c370377ab430110a1489d3fb7617f6`. Es una firma de desarrollo, no de distribución final.
- `testReleaseUnitTest`, `lintRelease`, `assembleRelease`: correctos; 129 pruebas, 0 fallos/errores/omitidas; lint 0 errores, 80 advertencias y 125 informaciones. No equivale a ausencia de deuda técnica. El límite de tamaño heredado de CI ya fallaba en el original y no se presenta como superado.
- Evidencia local: `artifacts/build-background-review.log`, `artifacts/first-tcl/{candidate.json,signature.txt,badging.txt,manifest-tree.txt,manifest-review.json}`.

El APK original de referencia y los candidatos anteriores se conservan en artifacts; no se instalarán por accidente usando el nombre mutable `tcl-home-prototype.apk`.

## Revisión del manifiesto y efectos al abrir

Permisos declarados: INTERNET, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE y ACCESS_FINE_LOCATION heredado. La instalación propuesta no usa `-g`; el arranque no solicita ni concede ubicación automáticamente.

Cuatro actividades: MainActivity exportada; LauncherSettingsActivity, QuickSettingsActivity y HomeSettingsActivity internas. MainActivity declara HOME y LEANBACK_LAUNCHER para una futura selección oficial; declarar HOME no equivale a asignar el rol. No hay código de asignación automática. El acceso a HOME_SETTINGS requiere una acción explícita en los ajustes y no forma parte de esta prueba.

Cinco servicios heredados: HomeKeyService, InputKeyService, SleepService (protegidos por BIND_ACCESSIBILITY_SERVICE) y ClockDream/AerialDream (BIND_DREAM_SERVICE). Registrarlos al instalar no los habilita ni selecciona. No hay receptores estáticos, incluido BOOT_COMPLETED. El receptor dinámico de cambios de paquetes vive con la actividad. ThemeProvider usa la autoridad propia `pe.tclhome.prototype.theme` y exige WRITE_SECURE_SETTINGS al llamante; no concede ese permiso al launcher.

La apertura ordinaria de MainActivity no activa estos servicios. Las acciones de entradas y ajustes nativos quedan fuera de la primera prueba. El identificador de paquete no coincide con Google TV ni con componentes TCL; se instalará sin `-r` para fallar si el paquete propio ya existiese.

## ScenicBackdropDrawable: corrección y evidencia

La versión revisada sí llamaba a BitmapFactory.decodeResource con Options y copiaba la fotografía mediante Canvas; createBitmap era el destino del dibujo, no la fotografía terminada. El defecto confirmado era reducir también la imagen principal a 960×540.

Ahora se consultan las dimensiones originales, se elige un muestreo que conserve el detalle visible y se dibuja la imagen principal al tamaño de la interfaz, hasta el límite de la fuente. Para la imagen actual 1920×1275, el resultado es 1920×1080 con sample=1. La copia de desenfoque es independiente, de 160×90; no reemplaza al fondo nítido de Inicio.

`decodeMs` delimita la decodificación real de píxeles; la lectura de dimensiones queda fuera. `prepareMs` incluye lectura de dimensiones, decodificación, recorte, tintes y desenfoque. `blurMs` cubre el filtro y el velo final, sin incluir la reducción previa. No se usa el nombre ambiguo `retainedBytes`: `backgroundCpuBitmapBytes` suma exclusivamente getAllocationByteCount de la imagen principal y la copia borrosa. Fuente temporal y arrays del filtro se registran por separado. No incluye GPU, iconos, cachés, fuentes, objetos Java, temporales internos del decodificador ni PSS total.

Decodificación, recorte, tintes y filtro se ejecutan en el trabajador `home-photo-prepare`. Un token cancela resultados antiguos y referencias débiles evitan retener Activity/View durante el trabajo. onDestroy cancela la entrega, detiene la transición y retira el Drawable. Se reciclan los bitmaps temporales y los resultados nunca publicados; los ya mostrados quedan bajo propiedad del recolector/RenderThread para evitar usarlos después de recycle. Esto corrige riesgos de retención; una prueba breve no demuestra ausencia absoluta de fugas.

Hay un único registro numérico por preparación completada, en el trabajador; no se registra por fotograma ni por pulsación. Su efecto sobre el rendimiento físico todavía debe medirse. Un fallo conserva el fondo de respaldo y no publica un bitmap vacío.

Capturas reales de emulator-5554 a 1920×1080, con este APK:

- `artifacts/background-review/home-full-resolution.png`: fotografía visible y nítida, foco y nombre dentro del dock.
- `artifacts/background-review/library-blurred.png`: fondo borroso al bajar; sin título de Todas las aplicaciones.
- `artifacts/background-review/home-restored.png`: regreso al dock.

Una preparación observada: decode 232.94 ms, filtro/velo 9.02 ms, preparación 635.32 ms, CPU del trabajador 494.35 ms. Son datos del emulador, no de la TCL. Memoria CPU retenida por los dos bitmaps: 8,352,000 bytes; fuente temporal: 9,792,000 bytes; arrays: 115,200 bytes. Preservar Full HD cuesta aproximadamente 5.93 MiB adicionales frente al destino anterior de 960×540; se acepta este coste para conservar calidad visual.

En nueve ciclos de bajar/volver y abrir/salir de una app de prueba, hubo una sola decodificación del fondo en el mismo proceso. PSS inicial/3/6/9 ciclos: 48,518 / 54,492 / 54,695 / 55,997 KiB. Hay crecimiento observado: no se declara estabilización ni ausencia de fugas con esta muestra. Registros brutos y condiciones: `artifacts/background-review/`.

## Fondos proporcionados por el usuario

Se revisaron visualmente los seis JPEG originales de `C:\Users\milmil\Documents\TCL Walpaper`, sin modificarlos. Asignación preparada: 1 alba, 2 amanecer, 3 día, 4 atardecer, 5 anochecer, 6 noche. El conjunto tiene seis estados, frente a los cinco iniciales: el futuro horario puede dividir 05:00–09:00 entre alba y amanecer. Horas exactas adicionales pendientes de la fase de fondos.

Para cumplir la prioridad de probar el APK actual antes de nuevas funciones, **este candidato conserva la fotografía fija Hercules ya comprobada**. Los seis fondos personales y su cambio automático aún no se incluyen. No se presenta el modo horario como terminado ni se publican las imágenes personales en Git.

## Comandos concretos: solo tras autorización individual

ADB local: `C:\Users\milmil\AppData\Local\Android\Sdk\platform-tools\adb.exe`. El descubrimiento local encontró el puerto TLS `192.168.0.107:43209`, y el usuario autorizó y se completó únicamente la reconexión a ese destino. Verificar `adb devices -l` antes de cualquier ejecución. Si cambia la dirección, el puerto o la identidad, detenerse y comprobarlos; nunca elegir el primer dispositivo ni usar un transport_id antiguo.

En los comandos de PowerShell siguientes:

```powershell
$adb = 'C:\Users\milmil\AppData\Local\Android\Sdk\platform-tools\adb.exe'
$serialTcl = '192.168.0.107:43209'
$apkPrueba = 'C:\Users\milmil\Documents\ChatGPT\Apple tv en andriod tv\artifacts\first-tcl\launcher-first-tcl-b859afc8a5b3.apk'
```

1. Comprobar exclusivamente el paquete de prueba y el base (lectura, autorización separada para cada consulta):

```powershell
& $adb -s $serialTcl shell pm list packages pe.tclhome.prototype
& $adb -s $serialTcl shell pm list packages com.gathin.tvhome
```

2. Comprobar Inicio actual, antes y después (cada ejecución requiere aprobación):

```powershell
& $adb -s $serialTcl shell cmd package resolve-activity --brief --user 0 -a android.intent.action.MAIN -c android.intent.category.HOME
```

Esperado según el diagnóstico anterior: `com.google.android.apps.tv.launcherx/.home.HomeActivity`. Si cambia o aparece un selector sin resolver, detener la instalación y consultar. No establecer Inicio para corregirlo automáticamente.

3. Instalar exclusivamente el APK congelado, si el paquete de prueba está ausente y Google TV sigue siendo Inicio:

```powershell
& $adb -s $serialTcl install --user 0 $apkPrueba
```

Sin `-r`, sin `-g`, sin activar servicios ni ejecutar scripts del fabricante. Comprobar SHA-256 local inmediatamente antes.

4. Abrir como aplicación normal, con autorización propia y solo si la instalación devuelve Success:

```powershell
& $adb -s $serialTcl shell am start -W -n pe.tclhome.prototype/com.gathin.tvhome.MainActivity
```

No se añade categoría HOME. También se podrá abrir manualmente desde las aplicaciones de Google TV con la etiqueta TCL Home · Prueba.

5. Para volver, **pulsar Home en el mando TCL**: debe aparecer Google TV. Atrás dentro de nuestro launcher primero vuelve al dock, por lo que no es la salida de emergencia. Si hiciera falta actuar por ADB, pedir aprobación específica:

```powershell
& $adb -s $serialTcl shell input keyevent KEYCODE_HOME
& $adb -s $serialTcl shell am force-stop --user 0 pe.tclhome.prototype
```

Son dos alternativas independientes, no una secuencia automática.

6. Desinstalar únicamente nuestra aplicación, solo si el usuario lo solicita y autoriza:

```powershell
& $adb -s $serialTcl shell pm uninstall --user 0 pe.tclhome.prototype
```

En esta primera prueba Google TV permanece instalado y conserva Inicio. Comprobarlo mediante el resolver anterior y el botón Home. El ensayo posterior de HOME_SETTINGS y su reversión constituye otra autorización y otra prueba.

## Resultado de instalación y apertura autorizadas

Se verificaron localmente el destino ADB `192.168.0.107:43209` (G08/Smart_TV_Pro) y el SHA-256 del APK congelado. La consulta del paquete no devolvió coincidencias. El resolver confirmó Google TV: `com.google.android.apps.tv.launcherx/.home.HomeActivity`, prioridad 2. Los únicos comandos de escritura ejecutados en la TCL fueron la instalación y apertura previamente autorizadas, exactamente como se describen arriba.

La instalación devolvió `Performing Streamed Install` / `Success`. La apertura devolvió:

```text
Status: ok
LaunchState: COLD
Activity: pe.tclhome.prototype/com.gathin.tvhome.MainActivity
TotalTime: 2224
WaitTime: 2255
Complete
```

Los tiempos están en milisegundos y corresponden al arranque medido por am start -W; no son tiempos de respuesta del mando ni certifican fluidez. La confirmación visual y la salida con Home permanecen pendientes; las primeras métricas se detallan a continuación. No se ejecutó ninguna asignación de HOME, activación de accesibilidad, ajuste de pantalla/audio ni operación HDMI. Evidencia bruta: `artifacts/first-tcl/tcl-{package-before,home-before,install,launch}.txt`; metadatos del candidato actualizados con el estado de instalación.

Durante la preparación hubo dos errores locales de captura en PowerShell (colisión con la variable reservada HOME y ausencia de archivo cuando la salida del paquete era vacía). Ambos detuvieron la secuencia antes de instalar; se corrigió la captura, se repitieron únicamente las consultas autorizadas y se aplicaron las condiciones antes de la instalación. No implicaron cambios de configuración en el televisor.

## Primera muestra física de rendimiento y comprobación de Inicio

Tras autorización individual se ejecutaron el resolver HOME posterior, `dumpsys meminfo pe.tclhome.prototype`, `dumpsys gfxinfo pe.tclhome.prototype framestats` y `exec-out screencap -p`, todos contra `192.168.0.107:43209`. Google TV sigue resolviendo como Inicio, con la misma actividad y prioridad 2. No se alteraron estadísticas con reset ni se inyectaron pulsaciones.

| Dato | Muestra obtenida en la TCL |
| --- | --- |
| PID del launcher en esta lectura | 11241 |
| PSS | 67,703 KiB = 66.12 MiB |
| Swap PSS | 213 KiB |
| Inicio/MainActivity: fotogramas acumulados | 2,033 |
| Inicio/MainActivity: jank informado por Android | 57 / 2,033 = 2.80 % |
| Inicio/MainActivity: p50 / p95 / p99 | 12 / 30 / 42 ms |
| Proceso completo: fotogramas acumulados | 2,317 |
| Proceso completo: jank informado por Android | 67 / 2,317 = 2.89 % |
| Proceso completo: p50 / p95 / p99 | 13 / 30 / 48 ms |

Los percentiles proceden del histograma de gfxinfo, con su discretización; no se presentan como percentiles exactos de todas las filas del buffer circular de framestats. El bloque legacy ofrece otro criterio (5.85 % en MainActivity / 5.65 % global); se conserva en el bruto, sin mezclarlo con el jank principal. La sesión incluye uso manual previo y lanzamiento inicial, sin una ruta controlada ni reinicio de contadores. No permite atribuir los picos a una animación concreta ni comparar con el original. Los contadores de alta latencia de entrada no son una medida en milisegundos de respuesta del mando.

El launcher estaba sin visibilidad al recoger la muestra: gfxinfo informa MainActivity con visibility=8 y contextos detenidos. La captura, de 1920×1080, muestra otra aplicación. Por ello **no es una captura de nuestra interfaz**, no verifica el fondo/dock en hardware y no se publica como ejemplo del diseño. Tampoco se toma esta lectura de PSS en segundo plano como memoria en primer plano o como evidencia de estabilización. No se ha cerrado ni reabierto nada para preparar artificialmente esta muestra.

El coste de decodificación en TCL, CPU, térmicas, arranque caliente, respuesta del mando y evolución de memoria tras ciclos definidos siguen pendientes. No se ha instalado una base adicional ni se inventa una comparación física. Son necesarias nuevas lecturas autorizadas y la confirmación de que nuestro launcher está visible para completar el ensayo.

Datos brutos: `artifacts/first-tcl/tcl-home-after.txt`, `tcl-meminfo-initial.txt`, `tcl-gfxinfo-initial.txt`, `tcl-screen-first.png`; resumen derivado: `tcl-initial-measurements.json`.

## Primera revisión con el mando y mediciones pendientes

El usuario prueba primero el dock en las cuatro direcciones, OK/Atrás, nombres/iconos, bajar a la cuadrícula, abrir una aplicación y regresar. Comprobar foco, ausencia de recortes, negros y cierres. No abrir selectores HDMI ni cambiar audio/imagen. No elegir Inicio predeterminado ni servicios de accesibilidad en los ajustes del launcher.

Solo tras aprobaciones de lectura separadas se recogerán gfxinfo framestats del paquete, meminfo/PSS, PID y registros TCLHomePerf limitados al proceso. Guardar fotogramas válidos, presupuesto real según refresco, jank y p50/p95/p99 de duración; no confundir duración total con cada fase de render. Repetir condiciones y ciclos; no inventar percentiles sin datos. am start -W mide lanzamiento desde ADB, no latencia del mando físico. La latencia del mando necesita una medición específica con sus límites documentados. CPU por PID e información térmica, si el firmware permite leerla, requieren sus propias autorizaciones.

El inventario anterior no contenía `com.gathin.tvhome`. Se verificará de nuevo antes de comparar. Sin base instalada y medida bajo las mismas condiciones **no existe comparación física válida frente al original**. No instalar una base adicional sin autorización ni sustituir la comparación por cifras de Google TV o del emulador.

## Incidencias y prioridades

| Prioridad | Hallazgo | Estado |
| --- | --- | --- |
| P1 | Compatibilidad física, mando, Home de salida, cierres y render en TCL | Instalación/arranque correctos y Google TV confirmado como Inicio después; validación visual y botón Home pendientes |
| P2 | Colas de fotogramas en primera sesión física | Inicio p95 30 ms, p99 42 ms, jank 2.80 %; falta ensayo controlado y base física, no añadir efectos para compensarlo |
| P2 | Fondo principal reducido a 960×540 | Corregido a tamaño de interfaz limitado por fuente; cinco nuevas pruebas de cálculo |
| P2 | Métrica retainedBytes podía interpretarse como memoria total | Sustituida por alcance explícito de bitmaps CPU, temporales separados |
| P2 | Entrega de trabajo gráfico a una actividad destruida | Token cancelable y referencias débiles; descarte y liberación de temporales |
| P2 | Crecimiento PSS en muestra breve del emulador | Observado; pendiente de sesiones largas y medición física, sin declarar fuga ni estabilización |
| P3 | Fondos personales por horario y funciones avanzadas | Aplazados por decisión de probar primero el hardware |

Tras la prueba real se añadirán resultados, comandos autorizados, capturas y defectos reproducibles a este documento. Corregir los problemas importantes antes de nuevas funciones o animaciones.


## Revisión visual comunicada por el usuario

Se recibieron observaciones de iconos antiguos, reflejos recortados, marcador de reorganización desplazado, reloj/fecha, biblioteca transparente y textos en inglés. El usuario solicitó además calendario, saludo y fondos personales. La captura autorizada posterior sí mostró nuestra cuadrícula en TCL: `artifacts/first-tcl/tcl-home-user-review.png`. El filtro autorizado de registros para el PID observado 11241 no devolvió registros de preparación; no se deducen de ello tiempos de decodificación ni un fallo de imagen.

Las correcciones locales y sus límites se documentan en [PULIDO-02.md](PULIDO-02.md). Todavía no están instaladas en TCL y no deben atribuirse a las métricas físicas del APK B859. El usuario continúa usando sus aplicaciones; no se han hecho más consultas ni cambios en la TV fuera de la captura y lectura de log expresamente autorizadas.

## Comprobación autorizada de paneles nativos

Actualización posterior: EC8011 ya se instaló y abrió con autorización como app normal. Google TV sigue siendo HOME. Resultado y pendientes en [ACTUALIZACION-TCL-EC8011.md](ACTUALIZACION-TCL-EC8011.md). Las métricas B859 anteriores siguen siendo históricas.

Posteriormente el usuario autorizó consultas de metadatos de los paquetes Google TV y su panel ambiental. Se identificó `com.google.android.apps.tv.dreamx` y su acción `com.google.android.libraries.tv.smarthome.intent.action.OPEN_SMART_HOME`, exportada y sin permiso requerido en el resultado. El panel nativo de notificaciones de `com.google.android.apps.tv.launcherx` sí exige STATUS_BAR_SERVICE; no se intenta eludirlo.

Con autorización específica se ejecutó una única apertura de Google Home: `adb -s 192.168.0.107:43209 shell am start -W -a com.google.android.libraries.tv.smarthome.intent.action.OPEN_SMART_HOME -p com.google.android.apps.tv.dreamx`. Devolvió Status ok/WARM, TotalTime 1473 ms, WaitTime 1587 ms. El usuario confirmó el panel y lo cerró con «Descartar». No hubo captura del panel, control de dispositivos domésticos ni cambios de configuración. Evidencia: `artifacts/first-tcl/google-home-open-test.txt`.

Esta prueba permite preparar el acceso desde nuestro Centro de control; aún debe validarse desde el APK ordinario en la siguiente actualización autorizada. No se ha actualizado la aplicación de la TCL ni cambiado su HOME. La revisión local siguiente se recoge en [CONSISTENCIA-03.md](CONSISTENCIA-03.md). Los avisos propios quedan apagados, con permiso exclusivamente manual y consumo activo pendiente de medir. Conservar launcherx y dreamx por sus funciones nativas y por la reversión a Google TV.


## Revisión física de EC8011 y prioridades (01/11/2026)

Se completaron únicamente las tres lecturas expresamente autorizadas después de la actualización: captura, meminfo y gfxinfo framestats. Brutos en `artifacts/tcl-review-06/physical/`. La captura es de nuestro proceso a 1920×1080; confirma iconos cuadrados interiores y no es una captura de Google TV presentada como propia.

PSS observado: **84,082 KiB (82.11 MiB)**; SwapPSS 11,105 KiB. La sesión acumuló **3,166 fotogramas**, 180 janky (**5.69%**), percentiles de histograma p50/p95/p99 **13/34/117 ms**. No se reiniciaron estadísticas ni se impuso un recorrido controlado; no se puede atribuir el p99 a una pulsación concreta, calcular latencia del mando ni compararlo directamente con el primer APK. El launcher base original no se instaló en la TCL para una comparación equivalente. No inventar esa referencia.

P1: eliminar cambio a foto ajena/fundido al abrir apps con vídeo; reducir el salto al bajar al catálogo; mantener liberación del decodificador. P2: uniformar arte de aplicaciones, subir etiquetas superiores, compactar Centro de control y colocar avisos debajo. La revisión local 06 aborda estos puntos y el sistema visual se recoge en [DESIGN-SYSTEM.md](DESIGN-SYSTEM.md). El nuevo APK todavía no está instalado en la TCL; allí sigue EC8011.

Las autorizaciones de estas lecturas ya se utilizaron. Una nueva captura, lectura, actualización, apertura o prueba automatizada en la TCL requiere autorización específica. No se han tocado HOME, permisos de notificaciones, accesibilidad, HDMI o políticas de audio. El consumo con NotificationListenerService autorizado continúa sin medir y no se recomienda activarlo todavía.


## Actualización autorizada 39264 · Ajustes jerárquicos

Se actualizó nuestra app en `192.168.0.107:37935` tras cinco autorizaciones específicas: conexión, presencia del paquete, HOME antes/después, instalación y apertura. El APK conservado `artifacts/stage-07/candidate-39264c9ee740.apk` se verificó localmente por SHA antes de ejecutar `install -r --user 0`. Resultado `Success`; apertura normal `Status: ok / COLD`, TotalTime 1450ms y WaitTime 1476ms. No es una comparación controlada con EC8011 ni una medida de respuesta del mando.

Google TV (`com.google.android.apps.tv.launcherx/.home.HomeActivity`) sigue siendo HOME, confirmado antes y después. Se conservan preferencias y servicios. Ninguna activación de accesibilidad/notificaciones, cambio de entradas o limpieza forma parte de esta actualización. [Cambios y pruebas del candidato](ETAPA-07.md), [caracterización del jank](JANK-07.md). Registro de la operación: `artifacts/stage-07/physical/update.json`.

Las afirmaciones anteriores de «EC8011 sigue instalado» describen ese checkpoint histórico; ahora está instalado 39264. Las métricas de EC8011/B859 no se atribuyen al nuevo APK. La revisión visual del usuario, frames, memoria y captura de 39264 están pendientes; las lecturas se han solicitado de nuevo. No convertir todavía el candidato en HOME principal sin una aceptación independiente y la reversión preparada.
