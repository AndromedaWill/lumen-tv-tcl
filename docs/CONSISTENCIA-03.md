# Consistencia visual, paneles y notificaciones opcionales

Revisión preparada el 31/10/2026, como continuación de [PULIDO-02.md](PULIDO-02.md). La TCL conservaba B859 durante su preparación. EC8011 se actualizó posteriormente con autorización específica y se abrió como aplicación normal; Google TV se verificó como HOME antes y después. Resultado físico y pendientes: [ACTUALIZACION-TCL-EC8011.md](ACTUALIZACION-TCL-EC8011.md).

## Presentación y recursos

- Biblioteca, calendario y Centro de control reutilizan `FrostedControlDrawable`: fotografía desenfocada precalculada, tinte y bordes comunes. La fotografía se dibuja opaca dentro del recorte antes del tinte; por eso no se transparentan las tarjetas que están detrás. Sin fuente preparada, se usa una base oscura opaca. No hay captura de pantalla ni desenfoque vivo por fotograma.
- La biblioteca elimina el texto repetido «En Inicio» y el indicador adicional de novedad. Conserva la marca verde para las aplicaciones añadidas y el estado en la descripción de accesibilidad. Tarjetas más compactas, con el nombre de la aplicación.
- `InstalledArtwork` proporciona el mismo recurso real de la actividad/aplicación instalada a Inicio y biblioteca. Se solicitan variantes de alta densidad, sin sustituir marcas por dibujos propios ni reutilizar banners antiguos. Las cargas del catálogo se reutilizan mientras no cambien los paquetes.
- Pedir mayor densidad no garantiza más detalle: en el APK de YouTube del emulador, el recurso más grande declarado es un PNG de **96×96**. Esa limitación puede seguir siendo visible al ampliarlo. No se ha examinado el APK de YouTube de la TCL ni se afirma que contenga el mismo recurso. Una variante más grande puede aumentar memoria y coste inicial; las mediciones deben decidir si compensa.
- Centro de control: Televisor/Red, Entradas/Inicio y Google Home/Notificaciones conservan etiquetas. La última referencia visual del usuario conserva las tarjetas grandes en forma de cápsula (radio 27 dp). Sonido/Imagen y Bluetooth/Aplicaciones forman dos parejas alineadas con esas columnas, en una sola fila de 52 dp: extremos exteriores redondeados y esquinas interiores de 4 dp, con separación interior de 6 dp. Se retiran círculos y la segunda fila compacta; queda espacio libre inferior para notificaciones. Se conserva el nombre al enfocar y la descripción accesible. Navegación explícita entre tres filas de dos accesos y la fila de cuatro segmentos. Se atenúa el fondo y el panel termina por encima del dock.

## Google Home y panel de avisos del sistema

Las consultas de metadatos fueron autorizadas individualmente. La acción `com.google.android.libraries.tv.smarthome.intent.action.OPEN_SMART_HOME` del paquete `com.google.android.apps.tv.dreamx` resuelve una actividad exportada y sin permiso requerido en la consulta del firmware real.

La prueba de apertura, autorizada por separado, devolvió `Status: ok`, estado WARM, `TotalTime: 1473` y `WaitTime: 1587` ms. El usuario confirmó el panel y lo cerró pulsando «Descartar». No se fotografió ese panel ni se accionaron dispositivos del hogar. Bruto: `artifacts/first-tcl/google-home-open-test.txt`. Esto valida su apertura desde ADB; el acceso desde el APK ordinario aún debe confirmarse en la próxima prueba física. El código comprueba exportación, habilitación y permisos y captura denegaciones; no elude controles del sistema. Cuando falta el panel se muestra únicamente «No disponible». Los títulos/subtítulos largos usan desplazamiento de texto al enfocar, solo si las animaciones están habilitadas; en modo reducido permanecen estáticos. No se permiten segundas líneas recortadas.

El panel nativo de notificaciones de `com.google.android.apps.tv.launcherx` exige `android.permission.STATUS_BAR_SERVICE`. No se intenta concederlo ni abrir ese panel protegido desde nuestra aplicación. Google Home no equivale a la categoría HOME de Android. **Conservar `dreamx` y `launcherx`**: son dependencias de funciones nativas, además de conservar la restauración a Google TV.

## Visor propio, apagado por defecto

Responsabilidades separadas:

| Componente | Función |
| --- | --- |
| `NotificationBuffer` | Hasta 20 avisos actuales, textos acotados, actualización/eliminación por clave, memoria únicamente. |
| `HomeNotificationService` | `NotificationListenerService` protegido por BIND_NOTIFICATION_LISTENER_SERVICE; callbacks del sistema tras autorización manual. |
| `NotificationsDialog` | Panel secundario, grupos por aplicación, iconos de origen, D-pad/OK/Atrás, configuración y pausa. |

Son dos decisiones explícitas: conceder el acceso especial en Android y activar el visor local. La preferencia local empieza en `false`. El launcher abre la pantalla oficial cuando está disponible; no concede acceso por ADB, no simula la aceptación del usuario y no usa accesibilidad. Si no hay pantalla compatible, informa y el launcher continúa funcionando.

Al pausar el visor se vacía el contenido y se solicita la desconexión del servicio. El permiso de Android permanece concedido hasta que el usuario lo revoque en «Gestionar permiso»; el texto lo explica. Un permiso concedido puede hacer que Android conecte inicialmente el servicio, que se desconecta si el interruptor local sigue apagado. No existe receptor de arranque ni servicio en primer plano.

Solo se conservan título, texto corto, paquete, clave y hora de los avisos actuales aceptados. No se guardan imágenes, RemoteViews, PendingIntents ni historial. No se escriben contenidos en archivos, registros o telemetría. Al eliminar un aviso, desconectar o destruir el servicio, se borra lo correspondiente. Preferencias guardan únicamente interruptores, nunca mensajes. Los iconos del panel están acotados a las aplicaciones actualmente mostradas y se liberan al cerrarlo.

Por defecto se omiten avisos permanentes, resúmenes de grupo, secretos y silenciosos. El usuario puede mostrar los silenciosos. **Estos filtros solo cambian la presentación; no limitan el alcance del permiso que concede Android.** El texto de permiso lo declara. No se sustituyen SystemUI ni el indicador de volumen. OK expande texto dentro del panel; no ejecuta acciones de otra aplicación ni descarta notificaciones del sistema.

No hay polling de notificaciones, temporizadores periódicos ni red. Las actualizaciones de pantalla se agrupan con una entrega como máximo cada 150 ms, solo con el panel visible. Los callbacks cuentan su tiempo de CPU y el número de avisos retenidos; el volcado del servicio no imprime contenido. La memoria del buffer está limitada por número y longitud de textos.

Referencia: [NotificationListenerService](https://developer.android.com/reference/android/service/notification/NotificationListenerService) y [consulta de autorización del usuario](https://developer.android.com/reference/android/app/NotificationManager#isNotificationListenerAccessGranted(android.content.ComponentName)). La desconexión local no revoca el acceso especial.

## Validación y alcance

La compilación ejecuta pruebas JVM, lint y ensamblado con Java 17/SDK 34. Resultado: **137 pruebas sin fallos**, lint sin errores, 95 advertencias y 150 informaciones. Las cinco pruebas nuevas del buffer verifican capacidad/orden, sustitución/eliminación, aislamiento de snapshots, textos acotados y filtros. Se retira la prueba de la política de sustitución de iconos que ya no existe.

Firma APK v2 válida para API 34, mismo certificado de desarrollo que B859. Paquete `pe.tclhome.prototype`, minSdk 26, targetSdk 34; cinco actividades, seis servicios declarados, ningún receptor estático y un proveedor protegido. El sexto servicio es el visor opcional; declarar un servicio no le concede acceso. Los cuatro permisos existentes no cambian. Sin bibliotecas nativas, compatible con el ABI armeabi-v7a del televisor mediante DEX, pendiente de nueva ejecución física.

Los brutos de la primera revisión se conservan en `artifacts/consistency-03/`, ignorado por Git. La primera verificación de navegación dio 25 comprobaciones correctas. Una ejecución de rendimiento quedó incompleta al terminarse el emulador y se excluye de cualquier comparación. Las capturas finales posteriores, que siguen la referencia de parejas de controles, están en `artifacts/control-pairs-05/`; no se sustituyen los brutos de diseños descartados por el usuario.

### Candidato final con controles por parejas

APK congelado: `artifacts/control-pairs-05/tcl-home-ec80115ddb25.apk`, 6.4 MB aproximadamente. SHA-256: `ec80115ddb254fb89c526cb0e0c8cb86c47d7798dda796fee2e6dddc73a084f4`. La firma y el manifiesto exactos están en `signature.txt` y `manifest.xml`; metadatos en `candidate.json`.

Capturas reales del APK en ejecución: `final/control-center-1080.png`, `final/control-center-720.png`, `final/compact-focused-{1080,720}.png` e Inicio en ambas resoluciones. Se verificaron **24 condiciones de geometría/foco/estado**: cuatro segmentos en una fila, alineación de cada pareja con la columna superior, texto «No disponible» completo, separación respecto al dock y espacio inferior, foco de Sonido, permiso sin conceder y servicio de notificaciones sin ejecutarse. El emulador se restauró a su resolución/densidad iniciales. Estos cambios de prueba no se aplicaron a la TCL.

El recorrido completo de D-pad y la pantalla del visor apagado pasaron **32 comprobaciones** con `tools/smoke-emulator.py`; resultados finales en `artifacts/control-pairs-05/smoke-verified/`. Un intento anterior del script envió flechas antes de que se abriese la ventana y se descartó; ahora espera y verifica el foco antes de entrar en el visor. Después del cambio de resolución también se detuvo un intento al detectar el Inicio del sistema en primer plano; se volvió a abrir explícitamente nuestra app y se repitió. No se aceptaron capturas de Google TV como si fueran del prototipo. No se concedió acceso a notificaciones ni se simuló una aceptación manual.

`LiquidGlassDrawable` conserva su ruta habitual para esquinas uniformes. Solo los segmentos usan trazados de esquinas asimétricas, construidos al cambiar sus límites y reutilizados al dibujar. `FrostedControlDrawable` aplica el mismo recorte a fotografía y acabado para que no queden reflejos cuadrados ni huecos de material. No se añaden bitmaps, desenfoques vivos o servicios para obtener estas formas.

Se deja libre el espacio inferior del Centro de control para avisos; no se colocan mensajes ficticios en las capturas. El visor existente sigue siendo una función secundaria con autorización manual, apagada. Su recepción real y consumo activo siguen pendientes de validar.

### Comparación repetible del emulador

Antes de la última corrección de geometría del Centro de control se instalaron por turno el candidato anterior 226AA9 y el intermedio FEE6A8, en el mismo arranque del AVD API 34 x86, 1920×1080/densidad 320. Misma configuración de usuario, cinco arranques fríos, cinco reanudaciones HOT desde la aplicación de prueba y el mismo recorrido de 48 pulsaciones. Sin compilaciones concurrentes durante las muestras, sin activar notificaciones. No es una comparación con el proyecto original ni con hardware TCL.

| Medida | 226AA9 anterior | FEE6A8 intermedio |
| --- | ---: | ---: |
| Mediana `am start -W` frío, 5 muestras | 651 ms | 590 ms |
| Mediana reanudación HOT, 5 muestras | 75 ms | 95 ms |
| PSS tras recorrido | 72,213 KiB | 64,765 KiB |
| Jank principal de gfxinfo | 22/786, 2.80 % | 24/801, 3.00 % |
| p50 / p95 / p99 de gfxinfo | 18 / 26 / 38 ms | 18 / 23 / 27 ms |

Las muestras HOT se solapan (65–114 y 71–113 ms). No se declara una mejora general del arranque ni ausencia de coste por solicitar recursos mayores: el frío y las colas del histograma bajaron, la mediana HOT subió 20 ms y el jank principal subió 0.20 puntos. Una ronda secuencial no separa todos los efectos de caché/variación del host. No se observa aumento de PSS en esta muestra; no demuestra estabilidad de memoria a largo plazo. Los percentiles son las categorías del histograma de Android, no percentiles exactos de latencia del mando. El muestreo breve de CPU en reposo informó 0.0 % en ambas versiones; no mide CPU durante navegación ni coste del servicio activo.

Brutos: `benchmark-before-verified/`, `benchmark-after-verified/`; resumen derivado `benchmark-comparison.json`. La versión anterior y esta incluyen ya los seis fondos. La fotografía principal conserva Full HD; no se redujo su calidad para mejorar las cifras.

**Consumo activo de notificaciones pendiente.** No se recomienda habilitarlo todavía: requiere primero una concesión manual y una medición de RAM/CPU con la misma carga, apagado/encendido, sin guardar avisos privados. Las pruebas con el visor desactivado no cuantifican su coste cuando recibe notificaciones. No se habilitó el permiso durante estas pruebas.

## Próxima prueba física

La instalación y apertura de esta prueba ya se ejecutaron con autorización, sin cambiar HOME. La revisión personal y las mediciones físicas nuevas siguen pendientes; el plan siguiente mantiene sus límites.

Pedir aprobación específica para actualizar únicamente `pe.tclhome.prototype` y abrirlo como aplicación normal. Comprobar Google TV como HOME antes y después mediante lecturas autorizadas. Probar iconos, biblioteca, círculos y los seis fondos; confirmar Google Home desde el APK sin tocar controles. Mantener notificaciones apagadas durante la primera comparación de rendimiento. No cambiar HOME, accesibilidad, HDMI, audio ni paquetes del fabricante.
