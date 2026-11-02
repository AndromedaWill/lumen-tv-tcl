# Revisión 08 · defectos observados en la TCL

Fecha: 2 de noviembre de 2026, America/Lima. Trabajo sobre el checkpoint `bc3a0a1b`. La TCL continúa con el APK 39264 instalado en la actualización anterior; el candidato de esta revisión se prueba únicamente en `emulator-5554` verificado.

## Evidencia física autorizada

Se utilizaron exactamente las cinco lecturas aprobadas: gfxinfo, meminfo, captura, dump del servicio propio y componentes autorizados como listeners. No se reiniciaron estadísticas ni se inyectaron teclas, abrieron paneles o modificaron permisos en la TCL.

Brutos: `artifacts/stage-07/physical/`. La captura muestra cómo las opciones de notificaciones se dibujaban encima de los botones, aunque el layout accesible pudiera informar límites correctos. Android conserva la autorización de nuestro listener, pero el dump de su servicio devuelve **pid=(not running)**. Por tanto, permiso concedido no equivale a conexión efectiva y la explicación «Visor activo» era incorrecta para ese estado. No se leyeron mensajes privados. No se concluye que el firmware sea incapaz de ofrecer notificaciones: falta comprobar una reconexión manual y recibir un aviso real.

| Muestra de uso libre, APK 39264 | Resultado |
|---|---:|
| Fotogramas acumulados | 3,335 |
| Jank moderno | 20.87% |
| p50 / p95 / p99 del histograma | 23 / 69 / 121 ms |
| PSS | 87,089 KiB (85.05 MiB) |
| Swap PSS | 20,407 KiB |

El anillo final conserva dos ventanas, 118 y 120 muestras válidas. UI p95: 1.356/0.915ms; emisión de comandos p95: 24.684/9.472ms. Son tramos transcurridos de CPU de un pipeline asíncrono, no tiempos puros GPU. No se asigna cada ventana a una pulsación o pantalla solo por su título MainActivity. La sesión no controlada no permite comparar porcentualmente con EC8011 ni con el emulador, diagnosticar una fuga o calcular latencia física del mando. Tampoco mide el coste de un listener activo: este no estaba ejecutándose. `user-review-summary.json` conserva el alcance.

## Defectos y correcciones

### Notificaciones: dibujo fuera del área y estado engañoso

El ScrollView limitaba su altura pero no garantizaba un recorte independiente dentro de un Centro de control que permite elevación fuera de sus filas. Al desplazar el contenido largo, su fondo y texto podían invadir los controles superiores. El viewport ahora tiene clip explícito, respeta la altura disponible y mantiene su cristal en el viewport, no en el contenido que se desplaza. Se añaden **8dp de separación** debajo de los botones; no se cambia la paleta ni la forma de los accesos.

El visor distingue autorización/preferencia de conexión real mediante `onListenerConnected`/`onListenerDisconnected`. Si no hay conexión, dice «Sin conexión al sistema» y permite **un intento explícito** de reconectar, pausar o gestionar el permiso. No se agrega polling, watchdog, concesión ADB ni autoactivación. No se amplían filtros para obtener mensajes a la fuerza. La declaración del servicio sigue exportada=false y protegida por BIND_NOTIFICATION_LISTENER_SERVICE, como el ejemplo oficial; no se cambia por una hipótesis sin verificar. Si la prueba física no entrega una utilidad real, retirar el visor es preferible a conservar una función engañosa.

[Contrato oficial de NotificationListenerService](https://developer.android.com/reference/android/service/notification/NotificationListenerService). El acceso no convierte en notificaciones estándar todas las tarjetas privadas del panel de Google TV.

### Vídeo: espera al abrir, brillo y cubierta retirada prematuramente

`beforeAppLaunch` retenía el lanzamiento hasta capturar y confirmar otro frame (96ms de plazo más turnos de UI; una ronda anterior llegó a 158ms). Ahora solicita una copia de mejor esfuerzo y ejecuta el lanzamiento inmediatamente. No se promete eliminar el arranque propio de Netflix u otra app ni el comportamiento de sus ventanas.

La cubierta estaba **encima** de las sombras superior/inferior y del oscurecimiento del catálogo. Al mostrarse se veía más luminosa que el vídeo. Ahora superficie y cubierta comparten las mismas sombras: superficie → cubierta → sombras → velo de catálogo.

Antes, MEDIA_INFO_VIDEO_RENDERING_START retiraba la cubierta inmediatamente, mientras PixelCopy todavía se ejecutaba. Ahora se conserva hasta verificar una copia válida de la superficie y el siguiente turno de animación. `VideoCoverState` invalida callbacks antiguos después de pausa, reinicio o cierre. Una copia tardía no puede retirar la cubierta durante el retorno. Dos intentos como máximo y sin reintentos estando fuera de Inicio.

El decodificador se libera en onPause. La SurfaceView conserva su composición hasta que la ventana queda oculta; en onStop pasa a invisible y libera la superficie. Así no se reproduce vídeo en segundo plano ni se elimina el hueco de composición mientras Android aún puede mostrar la salida. Los bitmaps publicados se retiran por propiedad/GC del render, sin recycle prematuro.

[PixelCopy](https://developer.android.com/reference/android/view/PixelCopy), [SurfaceView](https://developer.android.com/reference/android/view/SurfaceView), [MediaPlayer](https://developer.android.com/reference/android/media/MediaPlayer). Las condiciones de implementación se verifican con nuestro vídeo sin DRM; no constituyen control sobre ventanas protegidas de otras apps.

### Fondo predeterminado fugaz y movimiento del dock

Main reconstruía toda la Activity ante cualquier RESULT_OK de Fondos o Ajustes. Ahora el resultado distingue cambios de geometría/movimiento de cambios de fondo/nombre. PersonalBackground revalida el fondo al reanudar conservando RecyclerView, foco y scroll. Solo se reconstruye si cambian columnas/filas/efectos/tema o la familia de fondos heredados frente a la colección personal.

El arranque personal tampoco dibuja ya HorizonDrawable como escena ajena mientras carga la selección. Un gate de primer dibujo espera la imagen elegida o el póster del mismo vídeo. Es de una sola ejecución, con límite de 1500ms y cancelación al pausar/desconectar; un archivo roto no bloquea el mando indefinidamente. En un fallo frío se conserva la superficie neutra del sistema de diseño, sin sustituir por otra foto. La preparación sigue fuera del hilo principal. El gate no evita por sí solo las ventanas de arranque que gestione Android u otra aplicación.

Scenic congela la composición al pausar; finaliza las transiciones y libera la referencia anterior cuando la ventana queda oculta. Evita saltar de brillo a mitad de una transición antes de que la otra aplicación cubra Inicio.

### Primer Down desde el dock

Si el destino todavía no estaba creado, focusTile esperaba el final del desplazamiento de 480ms para enfocar. El aparcamiento dura ahora 280ms y se transfiere el foco cuando aparece el holder. Durante esa transferencia se evita iniciar otro desplazamiento desde el listener o desde el LayoutManager; el movimiento sigue teniendo un solo dueño. Los callbacks se invalidan al cambiar de destino o pausar. No se añaden capas hardware o imágenes para compensar la pausa. Distinguir duración intencional de transición, espera de foco y jank medido.

### Descargas accesibles de nuevo

El downloader no se había eliminado: quedó accesible solo desde un menú heredado ya fuera del flujo principal. Fondos → Añadir imágenes o vídeo → Descargar vídeos aéreos vuelve a exponerlo. `AerialStoreDialog` reutiliza AerialDownloads con filas virtualizadas, material compartido y selección mediante el mando. No crea un segundo downloader ni modifica la colección automáticamente al acabar de descargar; OK en uno guardado permite añadirlo sin duplicarlo.

El catálogo oficial se comprobó desde el host: **114 entradas de 1080p**, con el certificado raíz Apple ya incluido y verificación TLS/hostname, sin aceptar certificados inválidos. La confianza genérica del host no incluía esa raíz; la comprobación con la misma CA del proyecto sí pasó. No se descargaron vídeos nuevos ni archivos a la TCL. No es un catálogo servido por Lumen ni se incorporan recursos de Apple al APK.

Se conserva la reserva de 500MB y ahora se comprueba también durante transferencias de tamaño desconocido. Una respuesta vacía o incompleta no se renombra como vídeo terminado. La descarga es una tarea elegida por el usuario, no un servicio periódico. Los callbacks del menú guardan una referencia débil; al cerrarlo no retienen la Activity durante una transferencia.

## Mando e Inicio: alcance real

La APK física 39264 tiene settings_key_keycodes vacío y settings_key_panel=false. No se presenta como si ya remapeara el engranaje. En el candidato, Main responde a SETTINGS (176), NOTIFICATION (83) y códigos adicionales configurados **solo si Android entrega la tecla a nuestra Activity en primer plano**. Abre el mismo Centro de control. No se habilita HomeKeyService ni se altera el volumen.

Elegir HOME oficial en una segunda prueba debería asignar Home/Inicio a nuestro launcher; no garantiza que el firmware deje de capturar el engranaje, botones de apps, encendido, voz, entradas o volumen. Esos botones conservan sus servicios nativos. La TCL aún usa Google TV como HOME, comprobado antes y después de la actualización anterior. No se cambia en esta revisión.

## Verificación y límites

El checkpoint compila con Java17/SDK34, pasa **162 JVM**, lint **0 errores/122 advertencias** y firma v2 válida API34. APK conservado: `candidate-2f6019698468.apk`, SHA256 `2f6019698468e4ab393257c2cfd54f501542a96ece622c6c5224d3c388901c1a`. Ninguno de los candidatos de esta revisión está instalado en la TCL. Manifiesto sin nuevos permisos ni servicios; listener exportado=false.

Las cohortes se conservan por hash: 054EB626829C pasó 60 comprobaciones de Ajustes, 36 de Inicio y el recorte de notificaciones en 720p/1080p. 4B7A13019C20 añadió retención del fondo anterior hasta que el seleccionado esté listo y pasó dos ciclos con un vídeo propio, liberación del reproductor fuera de Inicio, regreso con foco y cambio de fondo sin mover el dock. 2F6019698468 solo añade respecto a 4B7 el borrado confirmado de descargas aéreas, bloqueado mientras la colección las referencia; su flujo UI queda pendiente de la siguiente ronda. No se presentan las pruebas de un hash como si fueran de otro.

La grabación real `continuity-4b7/transition.mp4` dura 20.09s (codificación 1280×720, UI 1920×1080). Detectó **un fotograma negro de 14.2ms al entrar en la aplicación de prueba**; no se detectaron intervalos negros en los dos retornos grabados. El siguiente fotograma pertenece a la aplicación de prueba. No se promete eliminar las ventanas/transiciones entre tareas controladas por Android: desde Android 11 la app no puede personalizarlas globalmente. [Contrato Activity](https://developer.android.com/reference/android/app/Activity).

ABBA repetible en el mismo emulador, fondo fijo Día, 12 ciclos Down/Atrás y sin grabación simultánea:

| APK | Fotogramas | Jank | p50/p95/p99 ms | PSS KiB |
|---|---:|---:|---:|---:|
| 39264 A1 | 510 | 19.22% | 26/48/53 | 41020 |
| 4B7 B1 | 419 | 20.76% | 25/48/53 | 41136 |
| 4B7 B2 | 426 | 19.48% | 25/48/53 | 41125 |
| 39264 A2 | 514 | 17.32% | 25/48/53 | 40973 |

El desplazamiento más breve genera menos fotogramas y el foco deja de esperar al final, pero **no demuestra una mejora del jank ni de p95/p99**. Sigue abierta la optimización del renderizado. No confundir esta medición con el uso libre de la TCL ni con el proyecto original. Brutos en `navigation-abba/`.

SETTINGS 176 abrió el Centro propio en el emulador; NOTIFICATION 83 fue capturado por Android antes de la Activity y abrió su panel nativo. Se documenta ese resultado negativo, sin activar accesibilidad para forzarlo. Un incidente del host dejó suspendidos los hilos QEMU; se conservó el diagnóstico y se reinició el mismo AVD sin borrar datos. Las rondas aprobadas y ABBA se ejecutaron después del reinicio. Las pruebas fallidas se conservan.
