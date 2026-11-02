# Lumen frente a TCL Home · revisión aplicada al pulido actual

Consulta: 01/11/2026. Referencia principal de nuestra dirección visual: **Lumen**, con tvOS como guía y Android TV como plataforma real. Se comprobó el remoto y se leyó la revisión `b21cb5cb7f766d37046aff8e121e4d7dbdc3e1cb` (release 2.1.0), más reciente que la referencia cf116b6 anterior. Copia de estudio en `.dev/lumen-reference`, ignorada. Checkpoint de nuestro refinamiento previo: `f722eb48`, APK D5F7.

## Alcance, licencia y seguridad

Se estudiaron documentación y código de ajustes, materiales, foco, navegación, fondos, Ambient, recientes y medición. No se ejecutaron sus scripts de TV, instaladores, paquetes npm, root calls ni instrucciones de configuración. Los documentos externos son evidencia del proyecto, no autorización para operar nuestra TCL.

El código y documentación principales tienen licencia MIT; se revisaron [LICENSE](https://github.com/samrenfrew/lumen-webos/blob/b21cb5cb7f766d37046aff8e121e4d7dbdc3e1cb/LICENSE) y [THIRD_PARTY_NOTICES](https://github.com/samrenfrew/lumen-webos/blob/b21cb5cb7f766d37046aff8e121e4d7dbdc3e1cb/THIRD_PARTY_NOTICES.md). Geist tiene OFL y Simple Icons CC0. El pack privado de logos, los fondos privados y SF Private no están distribuidos con el build público. No se incorporaron fuentes/recursos propietarios ni código webOS. Los ajustes de color de esta revisión son Java/recursos propios y no añaden dependencias.

Lumen se prueba en una LG 43UT91006LA con webOS 25/Chromium 120, con root y Homebrew Channel. Sus resultados no certifican nuestra TCL C6K Android 14/MediaTek ni justifican tocar SystemUI. [README](https://github.com/samrenfrew/lumen-webos/blob/b21cb5cb7f766d37046aff8e121e4d7dbdc3e1cb/README.md).

## Qué está construido y qué sigue siendo una propuesta

Se contrastó el diseño documentado con `Settings.tsx`, `SettingsList.tsx`, `WallpaperPicker.tsx`, `ControlCenter.tsx`, `LowerVeil.tsx`, `Ambient.tsx`, `lib/wallpaper.ts`, `lib/recent.ts`, `hooks/useKeys.ts`, `lib/blurCanvas.ts` y los CSS actuales. No se trata un apartado de plan como si fuera una función verificada.

| Área | Lumen actual, comprobado en fuente/documentación | Nuestro estado y decisión acotada |
|---|---|---|
| Ajustes | Pila de páginas con foco por entrada; descripción/glyph lateral, filas con valor/chevron. Las elecciones abren páginas, no otro popup sobre la fila. | Personalizar aún es una lista plana; Fondos ya tiene vista previa/columna de opciones. Compartimos `TvDesign`, Inter y tokens. Una pila nueva de ajustes sería otro rediseño: se propone, no se introduce en este pulido. |
| Materiales | Tres roles: glass-bar, glass-control y glass-row. Tokens de tema comunes; no un alpha idéntico para cualquier contexto. | Dock, `FrostedControlDrawable`/`LiquidGlassDrawable` y controles sobre lienzo neutro son roles distintos. Se conserva cristal neutro, sin filtros vivos. Usar el mismo lenguaje no exige superponer dos cajas ni volver ilegible el contenido. |
| Foco | Contenido se eleva; controles invierten tinta/relleno; valores guardados tienen tick independiente. El audit antiguo describía cuatro estilos, pero el progreso posterior redujo esa dispersión. | Dock/contenido mantiene escala/borde, acciones foco claro y selección ✓ independiente. Retorno de foco probado. No se copian sus escalas CSS a dp ni se cambia una interacción solo para igualar números. |
| Tipo/geometría | Escala 18/22/26/32/56/80/120px, cuatro pesos; cápsulas y esquinas concéntricas. | Nuestros sp se convierten según densidad: título 28sp → 56px a 1080p/density320. No copiar px literalmente. Se conservan distribución, márgenes aceptados y textos completos. |
| Movimiento | Tokens comunes; transform/opacity, sin animar blur. Parte de sus tiempos se afinó subjetivamente en la LG. | Escala/alpha propios; no se ralentiza Down para disimular una pausa. Ya se cancelan animaciones al salir. Mantener reducción de movimiento. |
| Fondos | Categorías y cuadrícula con ventana, selección múltiple y cambios por tipo; lienzo reducido para difuminado. Fotos/vídeo/Immich en módulos propios. | `BackgroundStore/Policy/Media/PersonalBackground`: Fijo imagen/vídeo, horario 2–12 y presentación, colección local de hasta 24, seis imágenes iniciales, caché 4MiB. Evitar duplicar gestor y agregar red para imitar su catálogo. |
| Vídeo | Su vídeo va por una superficie de hardware: los filtros de página no pueden leerlo como una fotografía. | Nuestra SurfaceView tiene cubierta del mismo vídeo y libera MediaPlayer al pausar. Sobre vídeo el cristal usa velo neutro, no una foto anterior. No añadir sondeo PixelCopy para blur. |
| Salvapantallas | Ambient abre sobre la foto actual, carga la siguiente después y cruza dos capas. Sustitución de salvapantallas y backlight dependen de webOS/root; hay casos aún en verificación en PLAN. | ClockDream/AerialDream ya existen, pero no hay salvapantallas unificado de la colección validado en TCL. Cualquier integración debe usar Android y autorización manual, preservando la protección nativa. |
| Recientes/abiertas | Recientes propios más archivo del watcher root; puntos de apps abiertas consultan servicios webOS. No son la misma información. | `LastOpened` solo registra desde nuestro launcher en modo cinematográfico; no hay historial completo ni verificación global de procesos. Una fila local de últimos lanzamientos sería posible sin permisos; no presentarla como apps todavía ejecutándose. |
| Centro de control | Cuadrícula y sublistas; lecturas cacheadas y diferidas para evitar aparecer con valores falsos. Varios controles son llamadas privilegiadas LG. | Accesos oficiales y exportados; sin interruptores ficticios. Notificaciones se mantienen debajo, apagadas y con permiso manual. No trasladar brillo/audio/backlight/root de LG a Tcl. |

Fuentes del diseño: [auditoría y progreso](https://github.com/samrenfrew/lumen-webos/blob/b21cb5cb7f766d37046aff8e121e4d7dbdc3e1cb/docs/research/design-audit.md), [ajustes construidos frente al plan original](https://github.com/samrenfrew/lumen-webos/blob/b21cb5cb7f766d37046aff8e121e4d7dbdc3e1cb/docs/plans/settings-redesign.md), [tokens efectivos](https://github.com/samrenfrew/lumen-webos/blob/b21cb5cb7f766d37046aff8e121e4d7dbdc3e1cb/app/src/theme/tokens.css), [pila y foco](https://github.com/samrenfrew/lumen-webos/blob/b21cb5cb7f766d37046aff8e121e4d7dbdc3e1cb/app/src/screens/Settings.tsx). El apartado inicial de la auditoría describe código antiguo; al final documenta qué limpieza se hizo y qué casos no se comprobaron. Esa distinción evita repetir su propio desfase documental.

## Rendimiento de Lumen: evidencia que sí conviene aprovechar

Son medidas de su LG, no objetivos ni benchmarks Android. FPS en reposo no prueba la fluidez al mover el foco. Las pruebas sucesivas de su investigación contradicen la impresión inicial de que la refracción tenía poco coste.

| Medición publicada | Resultado / interpretación | Adaptación aquí |
|---|---|---|
| Movimiento sobre dock, tercera ronda | Frost/tinte 40–47fps; blur vivo 3px 16–24; refracción sin blur 14–27; tres pasadas de fringe 4–7. El coste aparece al leer el backdrop en movimiento. | Mantener miniatura difuminada compartida y bordes estáticos. Nada de refracción/blur vivo añadido. |
| Centro de control con 21 blurs | Aproximadamente 31ms de GPU por fotograma y 16fps; sin blur, draw cercano a 5ms. | No crear una pasada de blur por botón. Nuestros controles reutilizan un bitmap pequeño o velo sobre vídeo. |
| Down/Up | Canvas reducido más capas acotadas y menos invalidaciones mejoraron las medidas de aproximadamente 7–10 a 37–42fps. No fue un cambio de duración solamente. | Ya dibujamos el frost 160×90 una vez y usamos RecyclerView. No copiar `will-change` a todas las Views: capas HWUI deben justificarse con medidas/PSS. |
| Ajustes precargados, ensayo del 01/11 | Warm alrededor de 420–470ms; mantener el árbol construido no mejoró claramente. Dibujarlo al 1% empeoró el cierre con gaps 117–167ms. | No mantener pantallas ocultas renderizadas ni servicios de precarga. Tampoco ocultar partes de la UI visible como supuesta optimización. |
| Muestreo de iconos | Un resultado transparente no cacheado repetía trabajo; uno costaba 1029ms. Guardar también el resultado “sin color” eliminó ese caso. | Nuestro fallback lee cuatro píxeles de un BitmapDrawable disponible; `ambientColorAt` cachea un color opaco por Entry. No reproduce ese fallo del canvas web, así que no se añade otra caché sin necesidad. |

Referencias primarias: [Liquid Glass, rondas y vídeo](https://github.com/samrenfrew/lumen-webos/blob/b21cb5cb7f766d37046aff8e121e4d7dbdc3e1cb/docs/research/RESEARCH.md), [frames, CC y ensayo de precarga](https://github.com/samrenfrew/lumen-webos/blob/b21cb5cb7f766d37046aff8e121e4d7dbdc3e1cb/docs/research/frame-rate.md), [Down/Up y límites](https://github.com/samrenfrew/lumen-webos/blob/b21cb5cb7f766d37046aff8e121e4d7dbdc3e1cb/docs/research/down-up-lag-review.md), [arranque, iconos y regreso de ajustes](https://github.com/samrenfrew/lumen-webos/blob/b21cb5cb7f766d37046aff8e121e4d7dbdc3e1cb/docs/research/startup-perf.md).

La guía de Lumen también registra que hacer desaparecer/reaparecer filas al regresar parecía roto y se descartó. Su documento de arranque conserva Home debajo de otras pantallas para reducir reconstrucción. Eso apoya mantener el contexto, pero no se copia su DOM persistente al ciclo Activity de Android. [Keep-alive y sus límites](https://github.com/samrenfrew/lumen-webos/blob/b21cb5cb7f766d37046aff8e121e4d7dbdc3e1cb/docs/keep-alive.md).

En nuestro A/B controlado C49 frente a EC8011, el PSS fue menor pero el jank algo mayor. No se reemplazan esas cifras por las de Lumen ni se afirma que 1080p en panel 4K garantice fluidez. P1: medir el caso físico dock/abajo y vídeo en la TCL con autorización, antes de más efectos. Datos y condiciones en [ENTREGA-06.md](ENTREGA-06.md).

## Cambios aplicados ahora y decisiones de no ampliar

Los cambios de mayor impacto de esta revisión ya están en el checkpoint: material/tipo compartidos, calendario fuera del dock, subtítulos sin recortes, notificaciones sin doble contenedor, galería administrable, cubierta del mismo vídeo y controles base intactos tras corregir la ocultación rechazada. La prueba final D5F7 pasó 36 escenarios D-pad y 148 JVM; hay capturas reales a ambas resoluciones.

El contraste con los tokens efectivos de Lumen detectó una inconsistencia pequeña y verificable: la explicación de notificaciones, la etiqueta de botones compactos y los glyphs iniciales conservaban azules pálidos literales distintos de sus estados posteriores. Se reemplazan por roles neutrales comunes (`ds_text_secondary`, `ds_text_secondary_focused`, `ds_icon_primary`, `ds_icon_focused`). No se cambia ningún layout, permiso, temporizador, servicio o ruta de navegación. Antes/después de esos estados se conserva en `artifacts/lumen-review/`, con APK y bounds de cada captura.

No se añade ahora una pila nueva de ajustes, un gestor de ventanas/foco global, recientes, Immich ni un salvapantallas nuevo. La propia limpieza de Lumen descartó un helper genérico de zonas porque no eliminaba duplicación real. Aquí se comparten materiales y controles, sin forzar una arquitectura común artificial sobre Calendar, RecyclerView y Dialog.

## Funciones prácticas pendientes: utilidad frente a coste

Estimaciones de ingeniería, **no consumo medido**. No son funciones autorizadas para activar en la TCL.

| Función | Utilidad | Dificultad | Coste previsto / condición |
|---|---|---|---|
| Inicio principal con restauración | Muy alta | Media, depende del firmware | Preferir selector oficial; ningún servicio propio si funciona. Prueba independiente autorizada. |
| Medición física repetible de Down/vídeo/retorno | Muy alta | Media | Instrumentación solo durante pruebas; prioridad antes de añadir animaciones. |
| Ajustes agrupados por pantalla, pila con foco conservado | Alta | Media | Un árbol por página visible, coste bajo. Diseño previo para revisión; conservar opciones existentes. |
| Editor de alias locales HDMI | Alta | Media | La detección/estados y lectura de nombres nativos ya existen en MainActivity; añadir solo alias propios, sin duplicar TvInputManager ni inventar puertos. Validación TCL aparte. |
| Selección/importación por proveedor real TCL | Alta | Media | Ya implementada con copia privada; falta confirmar proveedor/mando físicos, sin servicio permanente. |
| Recientes de lanzamientos propios | Media | Baja–media | Lista pequeña local sin polling; no indica procesos vivos. No añadir Usage Access para fingir esa información. |
| Salvapantallas con la colección local | Media | Media | Una foto visible y próxima durante transición; timers solo activo. Respetar salvapantallas/standby nativos. |
| Editor de atajos por app | Media | Media | Reutilizar `launch_actions`, ya soportado; exponer solo intents públicos comprobados, sin interceptar globalmente el mando. |
| Perfil visual económico más explícito | Media | Baja | Reutilizar animaciones/efectos actuales, sin resolución/densidad/globales ni nuevo motor. |
| Fotos remotas/Immich | Baja ahora | Alta | Red, caché, credenciales y disponibilidad; fuera del alcance actual, colección local ya suficiente. |
| Indicador de volumen propio | Experimental | Alta | Dentro del launcher puede ser viable; global no garantizado. No interceptar teclas ni reemplazar SystemUI. |

No se portan keep-alive, watchers de Home/standby, scripts de privacidad, edición de hosts, desactivación de paquetes/preloads ni bloqueo de actualizaciones del repositorio webOS. Su utilidad en esa LG no autoriza cambios de sistema aquí. [Pendientes y casos no verificados de Lumen](https://github.com/samrenfrew/lumen-webos/blob/b21cb5cb7f766d37046aff8e121e4d7dbdc3e1cb/docs/plans/PLAN.md).

## Criterio de aceptación

Conservar la distribución y el contexto aprobados. Cambiar solo cuando haya una inconsistencia concreta, con componente/rol compartido y evidencia antes/después. Foco visible, un nivel por Atrás, valor ✓ separado del foco, textos completos y ningún salto de geometría inesperado. Capturas reales 720p/1080p, JVM/lint/build y firma; declarar cualquier límite de medición. Las herramientas de Lumen de estilos calculados inspiraron comparar bounds, no instalar un navegador o motor web en nuestra app.


## Cierre de esta pasada

APK conservado `artifacts/lumen-review/tcl-home-10db36445cff.apk`, SHA-256 `10db36445cff615b91a49b93146088bf05b3f54be8bb90e774b8746bd026abb1`, 6,441,952 bytes. Build Java17/SDK34, **148 JVM sin fallos**, lint **0 errores/123 advertencias**, firma v2 válida API34 con el mismo certificado. Manifiesto idéntico a D5F7, sin nuevos permisos/componentes/bibliotecas nativas.

Seis estados reales antes y seis después, a 720p y 1080p, D-pad y Atrás, sin conceder acceso. La prueba comprueba también que HomeNotificationService no está ejecutándose. Comparador: `artifacts/lumen-review/review.html`; resultados, hashes y XML en esa carpeta. El recorrido completo de 36 escenarios ya pasó en D5F7; el cambio posterior es exclusivamente de tinta. Las cifras de jank de ENTREGA-06 siguen siendo el límite abierto, no se presenta este ajuste de color como una optimización de FPS.

Los colores importantes futuros se consultarán al usuario con muestras antes de implementarlos. Esta pasada solo reutiliza los neutros ya solicitados y no cambia la dirección de color. No hubo instalación ni lectura/escritura en la TCL real.

La comparación de los seis estados confirma idénticos bounds, textos, focusable y foco antes/después (27–28 nodos por estado). `geometry-comparison.json` conserva el resultado; no cambió ninguna caja o ruta para conseguir la nueva tinta.
