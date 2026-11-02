# Centro de control: música y cobertura funcional

2 de noviembre de 2026. Revisión de código sobre el checkpoint `c409d38`. El centro pertenece al launcher; conservar Google TV instalado no concede a nuestro proceso los permisos de su panel privilegiado.

## Reproducción multimedia implementada

Antes no había MediaSessionManager ni tarjeta de reproducción en ControlCenterDialog: no se podía controlar Spotify desde él. Ahora `NowPlayingSessions` obtiene sesiones mediante MediaSessionManager, selecciona una en reproducción antes que una pausada y observa cambios con MediaController.Callback. Una sesión destruida desaparece y se elige otra disponible. No se abre una app para generar una sesión ficticia, ni se detecta música por AudioManager.isMusicActive: eso podría confundir otros sonidos con música.

`NowPlayingPanel` aparece debajo de los accesos y **los avisos aparecen debajo de la música**. Por petición posterior del usuario, toda la columna se desplaza junta dentro de un viewport fijo, incluidos los botones. Se separan las secciones por 8dp. Sin avisos se oculta todo el bloque: no se muestran píldoras «Sin notificaciones» o «Desactivadas». El acceso superior permanece para gestionar el visor. Reproducir/pausar, anterior y siguiente solo funcionan si PlaybackState declara esas acciones. Los botones no admitidos quedan atenuados y fuera del recorrido D-pad. Se conserva el foco al actualizar contenido y se recupera al desaparecer una sesión o el último aviso. No hay barra de progreso con timer ni nuevo color de diseño.

La caja exterior difuminada se retiró tras revisión visual. `EdgeFadeScrollView` conserva tarjetas independientes y enmascara solo las franjas de desplazamiento superior/inferior de 18dp. Reserva margen para que el foco no se atenúe ni recorte, sin blur por fotograma ni nuevos hilos. `ContextFrostDrawable` permanece en el diálogo breve del permiso para separar sus textos: PixelCopy de nuestra ventana a 160×90 y preparación única fuera de UI; no captura otras apps ni persiste imágenes. Al cerrar libera referencias y termina su hilo. El bitmap pequeño retenido ocupa 57,600 bytes; no incluye buffers de Binder, texturas, stacks o PSS total. Se evita el blur global entre ventanas: [AOSP documenta su coste y que puede deshabilitarse según GPU o contenido de vídeo](https://source.android.com/docs/core/display/window-blurs).

No se conserva música, carátulas o historial en archivos, preferencias o registros. La copia propia de carátula tiene como máximo 128×128 (64KiB ARGB); la entrega Binder/caché de metadatos del framework también tiene coste, por lo que 64KiB no es el PSS total. El escalado, consultas y callbacks de sesión trabajan en un hilo de prioridad de fondo, creado solo al abrir el centro con acceso disponible. Se eliminan observadores y referencias al cerrarlo o pausar Inicio. Cambios de posición sin cambios visibles no repintan la tarjeta. No se pide audio focus, se cambia volumen o salida, se ejecuta reproducción en nuestro proceso ni se añade un servicio permanente.

### Acceso manual y limitaciones

La API pública requiere MEDIA_CONTENT_CONTROL (privilegiado, **no solicitado**) o un NotificationListenerService autorizado por el usuario. Se reutiliza el componente existente, sin otro servicio, permisos ADB, accesibilidad o root. Ajustes del lanzador → Sistema → Reproducción multimedia explica el alcance y abre el selector oficial para autorizar/revocar. El permiso es amplio; consultar sesiones o filtrar avisos no reduce lo que Android autoriza. El interruptor del visor de avisos sigue independiente. Sin autorización el launcher funciona normalmente y conserva el estado discreto de notificaciones.

Una app puede detenerse al salir, publicar metadatos incompletos o rechazar un control. No se evita esa política ni se promete reproducción de YouTube Music en segundo plano cuando su versión/cuenta no la permite. Los avisos privados del panel Google TV tampoco equivalen necesariamente a notificaciones estándar del sistema. Dos sesiones reproduciéndose a la vez siguen la prioridad del sistema con estabilidad de la selección; no se añade todavía un selector de fuentes.

Fuentes primarias: [MediaSessionManager](https://developer.android.com/reference/android/media/session/MediaSessionManager), [MediaController](https://developer.android.com/reference/android/media/session/MediaController), [PlaybackState](https://developer.android.com/reference/android/media/session/PlaybackState), [NotificationListenerService](https://developer.android.com/reference/android/service/notification/NotificationListenerService). API usada disponible desde 21, proyecto min26/target34; sin dependencia Media3 para controlar sesiones ajenas.

## Lectura física y pruebas

La consulta autorizada `dumpsys media_session` detectó paquetes com.spotify.tv.android, com.android.bluetooth y com.stremio.one. Solo se conservaron los paquetes: el parser no reconoció el formato de estados/acciones de ese volcado y no se presenta como si los hubiera verificado. No apareció YouTube Music en esa muestra. No se guardaron títulos, carátulas o listas. No se enviaron comandos multimedia ni se actualizó la TCL.

La instrumentación usa sesiones reales del framework con metadatos propios de prueba. Solo se inyecta el descubrimiento de esas sesiones, porque el emulador no tiene autorización manual. No se conceden permisos para pasar las pruebas y no se añade una puerta Intent a producción. El APK de instrumentación es independiente y nunca se instala en la TV. Capturas nativas en artifacts/media-09; no son capturas de Spotify en la TCL. La validación física de descubrimiento autorizado, reproducción y pausa sigue pendiente de la actualización aprobada.

## Funciones importantes: estado y prioridad

| Función | Estado real | Prioridad / dificultad / coste |
|---|---|---|
| Música de otras apps | Implementada; falta validar con Spotify/YouTube Music en TCL | Alta / media / callbacks solo con panel visible |
| Ajustes nativos TCL, sonido, imagen | Accesos oficiales existentes; no interruptores privilegiados | Esencial / baja / sin proceso añadido |
| Wi-Fi/Ethernet | Acceso existente; tarjeta no muestra todavía estado real completo | Alta / baja / NetworkCallback solo mientras visible; no buscar SSID con nuevos permisos |
| Bluetooth/dispositivos | Abre ajustes oficiales; no enumera todas las rutas ni controla emparejamiento propio | Alta / media / preferir pantalla oficial; Android 12+ limita lectura sin permiso Bluetooth |
| HDMI/TV en directo | TvInputManager, hijos CEC, nombres del sistema, estado desconocido y selector nativo existentes | Esencial / validación física / sin fijar cantidad de puertos |
| Alias HDMI propios | Lee loadCustomLabel del sistema; falta editor local por ID | Media / baja / preferencias pequeñas, sin tocar configuración TCL |
| Google Home | Acción exportada identificada; su utilidad requiere configuración nativa real | Media / depende firmware / no confundir con HOME de Android |
| Notificaciones propias | Opcionales; permiso concedido no produjo servicio conectado en muestra física | Media / dependiente firmware / validar reconexión y aviso real antes de recomendar; retirarlo si no aporta valor |
| Game Bar / modo juego | Sin acceso dedicado verificado en este firmware | Alta para juegos / media / identificar interfaz exportada; conservar servicios TCL |
| Almacenamiento e información | Hay colector heredado; falta resumen claro en ajustes nuevos | Media / baja / lectura a demanda, no monitor permanente |
| Recientes propios | Pendiente; solo lanzamientos elegidos desde nuestro launcher | Media / baja / lista limitada de paquetes, sin Usage Access ni historial de otras apps |
| Salvapantallas | ClockDream/AerialDream existentes; configuración manual del sistema | Media / baja / respetar inactividad y ciclo de vida |
| Apagar/dormir/temporizador | Helper heredado depende accesibilidad apagada; no ofrecer apagado falso | Opcional / permisos altos / conservar botón físico y menú oficial; no habilitar helper |
| Selección global de salida de audio | Panel nativo existente; no reemplazado | Esencial conservar / privilegiada / no tocar eARC, CEC, Dolby o políticas de audio |
| Casting/AirPlay/DRM/actualizaciones | Servicios nativos preservados; no necesitan reimplementación en el panel | Esencial / validación física / no deshabilitar paquetes |
| Botón físico Ajustes | Respuesta local si llega a Main; Android puede capturarlo antes | Alta / depende firmware / HOME por sí solo no garantiza remapeo global |

Siguiente implementación recomendada tras esta prueba: estado de conexión sin permisos nuevos, alias HDMI locales y recientes limitados. Game Bar y salidas de audio requieren validación soportada por TCL. No se agregan todas esas funciones a esta versión ni se inventan controles que Android no permita.
