# Arquitectura del fork TCL Home

## Primera iteración

Conservar MainActivity como coordinador y los adaptadores existentes. Incorporar un fondo vectorial procedural original, de dibujo estático y sin temporizador; un almacén pequeño de foco; configuración segura del prototipo. No reescribir la navegación ni crear otro launcher paralelo. El identificador de prueba será `pe.tclhome.prototype`, distinto del original, sin sustitución de paquetes.

Capas previstas, extraídas gradualmente cuando exista comportamiento que probar:

- **Inicio:** MainActivity, AppAdapter, GridNavigation, Order, DockSlots. FocusStore persiste clave de aplicación y posición de respaldo, sin conservar Views.
- **Presentación:** TvFocusMotion, LiquidGlassDrawable, fondo original; todos los movimientos cancelables. Modo reducido ya existente, respetado por nuevos efectos.
- **Fondos:** BackgroundController coordina un único modo; SchedulePolicy pura calcula franja/próximo límite con hora local; ImageImporter importa con ACTION_OPEN_DOCUMENT; ImageDecoder acota tamaño; preferencias guardan cinco rutas internas y horario validado. Reutilizar WallpaperSize y ciclo de vida de WallpaperPlayer.
- **Centro de control:** reutilizar QuickSettingsActivity y settings_key_panel, abierto explícitamente desde Inicio. NativeSettings resuelve Intents públicos y activities verificadas. Estado de red con callback visible; Bluetooth como acceso si no hay permiso. Ningún interruptor ficticio.
- **Entradas:** extraer InputRepository de TvInputManager (callback durante visibilidad), InputLabelStore para alias por ID y InputLauncher para TvContract/ACTION_VIEW. Mantener CEC y reproducción en aplicación TCL, sin implementar TvInputService propio.
- **Volumen:** investigación aislada; sin listeners globales, accesibilidad ni overlays en esta entrega.

## Fondos por horario: contrato antes de implementar

Cinco límites editables, ordenados y sin intervalos vacíos: 05:00, 09:00, 16:00, 18:30, 20:30. Noche cubre medianoche. Guardar minutos locales y usar java.time con zona del televisor; sin Internet, GPS ni AlarmManager permanente. Al estar visible: programar solo el próximo límite. En pausa: cancelar runnable y trabajo pendiente; al reanudar y recibir TIME_CHANGED/TIMEZONE_CHANGED: recalcular. Cambios de fecha/DST se resuelven desde el reloj actual, no sumando siempre 24 horas.

Importación: MIME JPEG/PNG/WebP, inspección real de bounds, límite de bytes y píxeles, 16:9 validado con mensaje útil; copiar a archivo temporal privado y reemplazar atómicamente tras validar. Conservar imagen anterior ante URI revocado, poco espacio o archivo corrupto. ACTION_OPEN_DOCUMENT no requiere READ_MEDIA_IMAGES ni acceso general al almacenamiento. Capturar ausencia de selector en firmware TV y ofrecer alternativa de importación local documentada.

Una imagen decodificada en reposo; como máximo dos durante crossfade. Decodificar fuera del hilo UI, tamaño de la superficie (máximo inicial 1080p), no cinco originales 4K. Una generación descarta resultados de importaciones/cambios anteriores. Crossfade configurable y desactivable. Vídeo y transición de imágenes nunca simultáneos; liberar MediaPlayer al salir de Inicio. Vídeos existentes opcionales y condicionados a verificación, sin distribución de activos Apple.

## Centro de control y HDMI

Panel propio del launcher, no panel privilegiado Android. Arriba a la derecha; foco inicial explícito, Back cierra y devuelve foco, todos los elementos alcanzables. Wi-Fi/Ethernet, Bluetooth, sonido, imagen, apps, almacenamiento y ajustes nativos se ofrecen como accesos oficiales. Game Bar solo si se identifica una activity soportada/exportada en firmware real. No adivinar componentes ni lanzar menús de fábrica.

No asumir cuatro HDMI. Usar lista y estados reales de TvInputManager; separar `desconectado`, `desconocido` y `conectado`; agrupar hijos CEC con su padre; preservar customLabel y alias del usuario. Sin entradas: explicación y acceso al selector nativo solo si está disponible. Un emulador no valida rutas de hardware, DRM ni eARC.

## Iteración Lumen (31/10/2026)

Implementados `ControlCenterDialog` (vista, foco y accesos), `ReflectiveSurfaceDrawable` (luz diagonal), `ScenicBackdropDrawable` (decodificación única fuera de UI y copia de 160×90 para vidrio), `HomeFocusStore` y `SystemSettings`. `DockDecoration` dibuja el material sin cambiar la geometría de los iconos. La ruta original de fondos sigue disponible. La foto original añade unos 500 KB de APK. Tras corregir el destino principal a Full HD se conservan 8,352,000 bytes en los bitmaps CPU principal y difuminado a 1080p; no es el PSS total. Los seis fondos personales añaden unos 4.6 MB comprimidos, sin decodificarse todos a la vez. Sin timers para este fondo estático. El bitmap pequeño se comparte por actividad y no se crea por frame.

No confundir esta vista de accesos con los paneles privilegiados de Android: sonido/imagen/red/Bluetooth se abren mediante intents oficiales y sus controles pertenecen al fabricante. Los handlers ausentes tienen un fallback a ajustes oficiales o un aviso de indisponibilidad.

La revisión final usa LumenDockDrawable con transparencia real y geometría de TILES_FOR[6]. El fondo prepara foto y velo difuminado fuera de UI, sin filtros vivos por frame. AppAdapter conserva capas de hardware solo para las tarjetas visibles de las dos primeras filas mientras Inicio está activo; se liberan al salir/desconectar las vistas. El modo normal usa transformación 2D, con parallax/tilt reservado a advanced_effects. No se oculta un segundo renderizador opaco dentro de la caché del dock.


## Pulido y fondos personales

Implementados PersonalBackground (modo/fase y ciclo visible), DayPhase (horario local puro), BackgroundSettingsActivity e ImagePickerIntent (selección/importación privada con detección de stubs), BackdropSize/ScenicBackdropDrawable (preparación y descarte), CalendarPanel, TopBarPresentation, InstalledArtwork y PackageChanges. Se conserva WallpaperPlayer para fondos heredados y AerialBackdrop para vídeos, sin reproducción simultánea en modo personal. El editor de horas y la importación con un proveedor real de TCL siguen pendientes. Véase [PULIDO-02.md](PULIDO-02.md) para contratos, pruebas y limitaciones.

## Consistencia y avisos opcionales

Actualización 06 (01/11/2026): [DESIGN-SYSTEM.md](DESIGN-SYSTEM.md) y `design_tokens.xml` rigen el diseño nuevo. `TvDesign` comparte tipografía, lienzo, panel de cristal neutro, foco, selección y tiempos. `LumenArtwork` normaliza Inicio/biblioteca: vectores CC0 conocidos, otros recursos instalados y Xuper identificado por el nombre real antes de los alias. Esto actualiza la política de solo iconos instalados descrita en la iteración anterior.

`NotificationsPanel` sustituye al diálogo central y queda bajo los controles, con viewport acotado y observador solo mientras está adjunto. Permiso exclusivamente manual y estado inicial apagado; iconos limitados a los grupos actuales, liberados al cerrar, sin historial persistente.

`BackgroundStore` guarda una colección versionada de hasta 24 elementos, migrando las seis imágenes/importaciones anteriores. `BackgroundPolicy` valida horarios de 2–12 imágenes y orden de presentación. `BackgroundMedia` copia/valida archivos en un worker, prepara previews con caché de 4 MiB y permite terminar una importación necesaria durante la reconstrucción. Solo borra copias propias después de confirmar persistencia y verificar que ya no se usan. `BackgroundSettingsActivity` mantiene presentación y edición; `PersonalBackground` programa únicamente el siguiente límite o intervalo mientras Inicio es visible y revalida la hora al volver.

`AerialBackdrop` mantiene un MediaPlayer visible y lo libera en pausa. `VideoStillCover` prepara una imagen del mismo vídeo y captura su propia SurfaceView, sin sondeo; cubre antes de liberar. Una apertura desde dock/biblioteca programa un plazo de 96 ms para actualizar el fotograma y después deja dibujar la cubierta antes de lanzar; registra el tiempo total real por apertura. Ese plazo no limita el tiempo de respuesta del hilo UI si está ocupado. No carga la primera foto de la colección ni aplica el antiguo fundido de 700 ms. Al regresar espera MEDIA_INFO_VIDEO_RENDERING_START antes de retirar la cubierta. Las ventanas de arranque y políticas DRM de terceros quedan fuera de su control.

El modo 2D evita geometría del vidrio/hero ocultos, doble desplazamiento al bajar al catálogo y capas persistentes por tarjeta. Las capas explícitas quedan para efectos avanzados; el RecyclerView conserva dos filas de caché. La foto principal sigue hasta Full HD y el frost de 160×90 no reemplaza su detalle.

LumenArtwork comparte arte entre Inicio y biblioteca: vectores conocidos y recursos instalados para otras apps. FrostedControlDrawable comparte la miniatura neutral difuminada y un tinte translúcido acromático. Mientras se muestra la biblioteca se oculta solo el dibujo del catálogo que hay detrás, conservando su geometría y foco; se restaura al cerrar. Sobre vídeo no se muestrea una fotografía anterior. ControlCenterDialog separa seis accesos con texto y cuatro segmentos agrupados en dos cápsulas de media anchura; conserva navegación explícita y apertura de ajustes oficiales, sin interruptores simulados.

NativePanels valida actividades exportadas, habilitadas y accesibles. Google Home usa la acción encontrada y probada en el firmware TCL. El panel nativo de notificaciones exige STATUS_BAR_SERVICE y no se intenta abrir ni eludir. Mantener los paquetes Google TV launcherx y dreamx para restauración y funciones nativas.

NotificationBuffer, HomeNotificationService y NotificationsPanel implementan el visor secundario autorizado: permiso especial manual más interruptor local inicialmente apagado, hasta 20 avisos actuales en memoria y sin historial/contenido en archivos o registros. Solo Android conecta el servicio tras autorización; no hay arranque automático, grants ADB, accesibilidad ni servicio en primer plano. Pausar borra el contenido y solicita desconexión; revocar el permiso se hace en Android. Medir el coste activo antes de recomendarlo. Contratos y evidencia en [CONSISTENCIA-03.md](CONSISTENCIA-03.md).


### Material final y cubierta previa al lanzamiento

El material compartido pasa a cristal acromático: muestra la miniatura neutral con alpha 96/255, un tinte carbón de 74/255 y conserva el color original de la foto visible detrás. Se añaden 57,600 bytes para la variante neutral, preparada una vez en el worker: total de bitmaps CPU 8,409,600 bytes a 1080p. No hay un bitmap extra por control. El calendario se ancla por encima del dock, los textos prescinden de padding de fuente y los avisos resumidos carecen de caja exterior. Esta política actualiza los paneles opacos de la iteración anterior; ver DESIGN-SYSTEM.md.

VideoStillCover espera la copia acotada y publica la cubierta antes de iniciar la app, dando un fotograma al dibujo normal para que la transición no capture una raíz ajena o una SurfaceView ya liberada. `videoFreezeMs` incluye ese intervalo total. La espera de copia es de hasta 96ms más el fotograma de presentación; no es una garantía de tiempo real si el hilo principal está ocupado. Las muestras observadas variaron: 33ms en una versión previa, 158ms en la grabación final verificada y 47–113ms en cinco ciclos sin grabación. También se conserva una muestra de 310ms bajo carga. Son cifras de emulador, no de la TCL.

Los modales de Fondos y Personalizar conservan intactos los controles de su pantalla base. El oscurecimiento se aplica solo mediante la ventana y el token compartido; no existe un helper que oculte o atenúe individualmente sus vistas. La prueba visual comprueba también contexto visible y retorno de foco.
