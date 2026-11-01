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

Los tiempos están en milisegundos y corresponden al arranque medido por am start -W; no son tiempos de respuesta del mando ni certifican fluidez. La confirmación visual, la salida con Home y las métricas de ejecución permanecen pendientes. No se ejecutó ninguna asignación de HOME, activación de accesibilidad, ajuste de pantalla/audio ni operación HDMI. Evidencia bruta: `artifacts/first-tcl/tcl-{package-before,home-before,install,launch}.txt`; metadatos del candidato actualizados con el estado de instalación.

Durante la preparación hubo dos errores locales de captura en PowerShell (colisión con la variable reservada HOME y ausencia de archivo cuando la salida del paquete era vacía). Ambos detuvieron la secuencia antes de instalar; se corrigió la captura, se repitieron únicamente las consultas autorizadas y se aplicaron las condiciones antes de la instalación. No implicaron cambios de configuración en el televisor.

## Primera revisión con el mando y mediciones pendientes

El usuario prueba primero el dock en las cuatro direcciones, OK/Atrás, nombres/iconos, bajar a la cuadrícula, abrir una aplicación y regresar. Comprobar foco, ausencia de recortes, negros y cierres. No abrir selectores HDMI ni cambiar audio/imagen. No elegir Inicio predeterminado ni servicios de accesibilidad en los ajustes del launcher.

Solo tras aprobaciones de lectura separadas se recogerán gfxinfo framestats del paquete, meminfo/PSS, PID y registros TCLHomePerf limitados al proceso. Guardar fotogramas válidos, presupuesto real según refresco, jank y p50/p95/p99 de duración; no confundir duración total con cada fase de render. Repetir condiciones y ciclos; no inventar percentiles sin datos. am start -W mide lanzamiento desde ADB, no latencia del mando físico. La latencia del mando necesita una medición específica con sus límites documentados. CPU por PID e información térmica, si el firmware permite leerla, requieren sus propias autorizaciones.

El inventario anterior no contenía `com.gathin.tvhome`. Se verificará de nuevo antes de comparar. Sin base instalada y medida bajo las mismas condiciones **no existe comparación física válida frente al original**. No instalar una base adicional sin autorización ni sustituir la comparación por cifras de Google TV o del emulador.

## Incidencias y prioridades

| Prioridad | Hallazgo | Estado |
| --- | --- | --- |
| P1 | Compatibilidad física, mando, Home de salida, cierres y render en TCL | Instalación y arranque correctos; revisión visual con mando y métricas pendientes |
| P2 | Fondo principal reducido a 960×540 | Corregido a tamaño de interfaz limitado por fuente; cinco nuevas pruebas de cálculo |
| P2 | Métrica retainedBytes podía interpretarse como memoria total | Sustituida por alcance explícito de bitmaps CPU, temporales separados |
| P2 | Entrega de trabajo gráfico a una actividad destruida | Token cancelable y referencias débiles; descarte y liberación de temporales |
| P2 | Crecimiento PSS en muestra breve del emulador | Observado; pendiente de sesiones largas y medición física, sin declarar fuga ni estabilización |
| P3 | Fondos personales por horario y funciones avanzadas | Aplazados por decisión de probar primero el hardware |

Tras la prueba real se añadirán resultados, comandos autorizados, capturas y defectos reproducibles a este documento. Corregir los problemas importantes antes de nuevas funciones o animaciones.
