# Revisión 06 · fondos, transiciones y consistencia

Desarrollo local sobre EC8011. La TCL conserva EC8011; no se ha instalado este candidato ni cambiado su configuración. Véase [DESIGN-SYSTEM.md](DESIGN-SYSTEM.md) para auditoría visual, reglas reutilizables y aprobación pendiente.

## Galería funcional

Vista previa grande, colección de miniaturas, tres modos y edición con el mando. Las seis imágenes del usuario se conservan como colección inicial y sus originales no se modifican.

- **Fijo:** imagen o vídeo local. Una vista previa de vídeo es estática; se reproduce al volver a Inicio.
- **Por horario:** entre 2 y 12 imágenes incluidas, cada una con una hora distinta. La imagen sigue activa hasta la siguiente hora, incluso al cruzar medianoche. Se puede excluir una imagen de este modo y mantenerla en la colección.
- **Presentación:** imágenes de la colección en orden secuencial o aleatorio sin repetición inmediata; intervalos de 10 segundos a 24 horas, con presets y editor de minutos/segundos. Los vídeos quedan fuera de esta rotación.

La colección admite hasta 24 elementos. Desde OK sobre la vista previa o miniatura: usar fijo, reemplazar imagen/vídeo, editar hora, incluir/excluir, mover antes/después y quitar con confirmación. Importar no exige reconstruir el APK. El selector usa OPEN_DOCUMENT o un GET_CONTENT accesible y descarta el stub de Android TV; depende de que el firmware tenga un proveedor real manejable con el mando. La interfaz del selector pertenece al sistema, no al launcher.

JPEG, PNG o WebP horizontales, máximo 24 MiB por imagen y dimensiones/rango de aspecto comprobados; vídeo local máximo 256 MiB y hasta 4096×2160, sujeto a los códecs disponibles. Se valida una decodificación antes de guardar. Hay copia privada, no permisos de almacenamiento amplios ni dependencia de que una URI siga siendo accesible tras reiniciar. Un fallo conserva la colección anterior. Reemplazar/eliminar solo descarta copias del launcher, después de confirmar persistencia; no toca documentos originales ni los aéreos heredados. Las importaciones pendientes bloquean ediciones incompatibles y sobreviven a la reconstrucción de la pantalla.

El programador solo vive mientras Inicio está visible; se retira al salir y se revalida al volver o cambiar hora/zona. La presentación de una sola imagen no mantiene un temporizador. Hay caché acotada de previews y un worker de E/S, sin trabajo periódico del sistema.

### Referencias estudiadas

[Arc Launcher](https://github.com/meddouribadis/arclauncher) separa el gestor propio de fondos de la disponibilidad de WallpaperManager y ofrece imágenes, vídeo y presentación. Tomamos la idea de un gestor interno y opciones por modo, sin portar Flutter ni dependencias. [Projectivy, repositorio del desarrollador](https://github.com/spocky/miproja1), documenta fondos animados y fuentes configurables; inspira separar la colección del modo de reproducción. No se presenta su implementación como código abierto portado ni se añaden proveedores en red. Las tres reglas temporales y componentes Java de esta revisión son propios.

## Vídeo: causa y corrección

Había tres causas dentro de nuestro código: la cubierta podía dibujar el ColorDrawable negro de la raíz mientras WallpaperPlayer dibujaba la fotografía en otra vista; se liberaba el decodificador antes de preservar visualmente su superficie; y el modo de vídeo personal cargaba la primera foto de la colección como poster, aunque no tuviera relación con el vídeo. La transición inicial de 700 ms mezclaba esa imagen con el contenido recién reproducido.

Ahora el poster se extrae **del mismo archivo de vídeo**, fuera de UI. PixelCopy obtiene una captura acotada de la SurfaceView propia al comenzar y antes de abrir desde dock/biblioteca. Esa apertura espera como máximo 96 ms para actualizar la cubierta; el registro `videoFreezeMs` permite comprobar su coste real, que debe medirse también en la TCL. La cubierta se presenta antes de ocultar/liberar y permanece al regresar hasta que el reproductor anuncia contenido renderizado. No hay sondeo constante ni decodificador retenido detrás de otras apps. Se cancela el antiguo fundido de la cubierta sobre el vídeo y la transición de salida de los lanzamientos normales.

La captura es el último fotograma **disponible**, no una garantía del último fotograma exacto. En un arranque de proceso nuevo hay preparación inicial del vídeo; los snapshots de tareas, las ventanas iniciales de otras apps y la composición del firmware pueden influir. No se promete eliminar una pantalla negra dibujada por la aplicación de destino ni cambiar DRM. [Ciclo de vida MediaPlayer](https://developer.android.com/media/platform/mediaplayer/state-resources), [SurfaceView](https://developer.android.com/reference/android/view/SurfaceView).

## Iconos y navegación

`LumenArtwork` utiliza la composición de Lumen con color a toda la tarjeta, logo centrado y proporciones preservadas. Los paths CC0 conocidos se dibujan como vectores, sin ampliar PNG diminutos. Apps desconocidas conservan su recurso instalado con extensión del fondo y manejo de iconos adaptativos. Inicio y biblioteca usan la misma fábrica. El nombre real instalado identifica Xuper aunque el paquete heredado contenga «magis»; no se cambia una app Magis genuina a Xuper. El símbolo X de esta revisión es propio, no una marca oficial copiada.

Se evitan cálculos de hero/vidrio ocultos, la segunda orden de desplazamiento al bajar y capas persistentes innecesarias en modo 2D. El RecyclerView conserva dos filas. La fotografía continúa hasta Full HD; solo el frost usa 160×90. Las etiquetas superiores quedan 9dp más cerca de sus botones.

Centro de control sin título visible, con separación de los accesos superiores, botones menores y pares redondeados. Notificaciones debajo, sin diálogo central gigante ni bloque gris. Sin acceso, solo «Notificaciones desactivadas»; OK muestra explicación y acceso a configuración Android. El servicio no se activa automáticamente. Su consumo activo y sus notificaciones reales no se han probado porque no se ha concedido ese permiso.

## Evidencia local y límites

`artifacts/tcl-review-06/` contiene logs de compilación/firma/manifiesto, 148 pruebas JVM sin fallos y lint sin errores; las advertencias restantes se conservan en el informe. `qa/` contiene selección real de un vídeo H264 por el selector del emulador, edición de Noche 20:30 → 21:29, reordenamiento/eliminación hasta tres imágenes, persistencia tras pérdida de proceso y presentación aleatoria de 10 segundos. Las capturas del área fotográfica 11 segundos después son distintas; se restauraron después las seis imágenes y Fijo/Día.

`qa/video/transition.mp4` es una grabación real de la prueba, con un patrón H264 creado para diagnóstico, no una maqueta o un fondo comercial. No se detectó un intervalo de pantalla casi negra con el umbral documentado (98% de píxeles por debajo de 8% de luminancia, al menos 10 ms). `media.player` no muestra cliente de nuestro paquete mientras la app fixture está en primer plano y vuelve a mostrarlo al regresar. Esto valida el recorrido controlado en emulador, no el caso específico del firmware TCL. Debe repetirse allí con autorización.

Las capturas de diseño están en `artifacts/design-system/`, sin retocar y a resolución real de emulador. No confundir 1080p de interfaz con el panel físico 4K. Los percentiles, PSS y comparación controlada se guardan en las carpetas de benchmark; no derivar latencia del mando de un tiempo de proceso ADB. El APK base original no se ha instalado como referencia física equivalente.

Pendientes de hardware: selector de documentos real del firmware, vídeo observado por el usuario, nitidez de iconos de sus apps, recorrido de mando y métricas controladas. Google Home desde el acceso del nuevo APK también requiere revisión. HOME, accesibilidad, notificaciones activas, HDMI/CEC/eARC y volumen global no se modifican en esta entrega.
