# TCL Home · sistema de diseño v1

Estado: implementación de desarrollo, con refinamiento neutral solicitado después de revisar las primeras capturas; pendiente de aprobación visual y de una nueva prueba autorizada en la TCL. Revisión: 1 de noviembre de 2026. A partir de esta versión, las funciones nuevas deben usar estos componentes y justificar cualquier excepción.

## Alcance y evidencia

Android TV con Views/Java, API mínima 26 y objetivo 34; mando D-pad, OK y Atrás. Referencia visual tvOS 26, interacción y ciclo de vida Android. No es una certificación de conformidad con HIG ni una reproducción de SystemUI.

Capturas **reales**, sin retocar: `artifacts/design-system/before/{1080,720}/`, `refinement-before/{1080,720}/` y `after/{1080,720}/`. La segunda colección conserva la propuesta previa a la petición de más transparencia; la última es el refinamiento neutral. Cada carpeta incluye Inicio, Fondos, Calendario, Personalizar, Centro de control y notificaciones apagadas. Los pares `focused/resting`, `next/previous` y `controls/preview` cambian el foco; siempre debe existir un foco activo en televisión. Las capturas finales registran también el SHA-256 del APK instalado (C49EC0AE8566…, 22 estados entre ambas resoluciones). Los iconos de Archivos, Cine y Colección pertenecen a fixtures, no a aplicaciones de la TCL.

## Apple guidance

Apple aconseja arte a pantalla completa, legibilidad a distancia y un foco que destaque suavemente el elemento activo. Es la referencia para jerarquía, escala y movimiento; no prescribe nuestros valores Android. [Designing for tvOS](https://developer.apple.com/design/human-interface-guidelines/designing-for-tvos).

Los materiales deben separar funciones y contenido. Apple reserva Liquid Glass principalmente para controles y navegación y recomienda moderación; los fondos y el contenido textual requieren otro tratamiento. Adoptamos esa distinción sin implementar los efectos ópticos propietarios. [Materials](https://developer.apple.com/design/human-interface-guidelines/materials).

La alineación, los grupos y la revelación progresiva ayudan a entender las relaciones. La composición debe adaptarse al tamaño y al texto. [Layout](https://developer.apple.com/design/human-interface-guidelines/layout). Los tamaños y pesos expresan jerarquía y deben poder leerse desde lejos. [Typography](https://developer.apple.com/design/human-interface-guidelines/typography). El feedback debe ser breve y respetar la reducción de movimiento. [Motion](https://developer.apple.com/design/human-interface-guidelines/motion).

## Inference

La mezcla de selectores de distintos estilos reduce la coherencia. Conviene compartir material y foco, conservando las distribuciones conocidas. El usuario rechazó tanto los reflejos marrones como los paneles azul oscuro y la doble caja de avisos. La solución es un cristal neutro con menor cobertura del difuminado. Se evita aplicar un tinte cálido; puede seguir apreciándose el color natural de la fotografía por la transparencia. La segunda petición del usuario prioriza recuperar la transparencia inicial frente a una plancha gris opaca. El brillo de foco no debe invadir esquinas ni sustituir una indicación inequívoca de selección.

### Auditoría previa

| Pantalla / evidencia | Hallazgo | Prioridad | Corrección acotada |
|---|---|---|---|
| Inicio, Runtime + Static | El dock ya tiene composición aceptada; iconos instalados cuadrados pueden crear otro marco interior. Hay fixtures en emulador. | P1 | Una sola fábrica de arte para Inicio y biblioteca; vectores nítidos de la referencia Lumen cuando existen, color a toda la tarjeta y logo centrado. No inventar que todos son iconos originales de Lumen. |
| Fondos, Runtime | Título de 30sp frente a 34sp en Personalizar; controles y diálogos fabricados localmente. Vista previa y miniaturas ya aportan la jerarquía solicitada. | P2 | Título y controles compartidos; preservar vista previa grande, modos y colección. |
| Calendario, Runtime + Static | Material fotográfico muy presente; mes, flechas y día actual usan reglas distintas de los controles. | P2 | Cristal neutro compartido, misma fuente y foco de flechas; hoy mantiene un indicador diferente del foco. |
| Personalizar, Runtime | Filas de 52dp y título de 34sp; selector gris y margen de 72dp producen otra escala visual. | P2 | Título 28sp, filas 44dp, margen 36dp y controles comunes; marca ✓ para valor guardado, foco independiente. |
| Centro de control, Runtime | El borde superior de los botones invade la zona de accesos redondos; tamaño y separaciones pueden compactarse. | P1 | Sin título, desplazamiento inferior y botones menores; pares de accesos con extremos redondeados; notificaciones debajo. |
| Notificaciones, Runtime | Bloque gris grande y texto redundante «Configurar». | P2 | Cristal compartido; estado compacto «Notificaciones desactivadas». OK revela la explicación y configuración. Sin avisos ficticios. |
| Biblioteca, Runtime + Static | Necesitaba aislamiento del catálogo e iconos idénticos a Inicio; el check hace redundante «En Inicio». | P2 | Panel compartido, catálogo oculto mientras está abierto y fábrica de arte común, sin repetir el estado en texto. |
| Apertura con vídeo, Runtime + Static | Se cargaba una fotografía ajena como respaldo y se mezclaba con el vídeo; la superficie desaparecía al liberar el decodificador. | P1 | Fotograma del mismo vídeo, captura acotada antes de abrir y cubierta antes de liberar. Sin fundido a una foto de la colección. |

## Project choice

### Tokens y unidades

La fuente de verdad de los valores es `launcher/app/src/main/res/values/design_tokens.xml`. Los números son decisiones de este proyecto, no equivalencias entre puntos de Apple y dp de Android. La fuente es **Inter**, ya licenciada bajo OFL; no se incorpora SF Pro ni SF Symbols.

| Rol | Valor / regla |
|---|---|
| Lienzo | Gradiente carbón neutro `#202020` → `#111111`; Inicio conserva la fotografía o vídeo elegido. |
| Panel de información | Cristal neutro y borde tenue. Calendario, biblioteca y diálogos comparten material; no hay planchas azul oscuro. |
| Controles sobre foto | Cristal con frost de la foto **desaturado y atenuado una vez en el worker**, compartido y dibujado al 96/255 de alpha. El tinte neutro usa 74/255 de alpha; la foto puede percibirse a través del material. No hay blur en vivo ni filtro de color por botón. |
| Texto | Principal `#F5F5F5`, secundario `#D2D2D2`, enfocado `#202020`. |
| Foco de acción | Blanco suavemente translúcido `#D4F4F4F4`, texto oscuro; elevación discreta donde ya existe. Nunca un halo blanco exterior. |
| Selección persistente | Gris translúcido `#30545454` y marca ✓; no confundir el valor guardado con el control enfocado. |
| Escala tipográfica | Título 28sp, sección 20sp, cuerpo 16sp, control 14sp, compacto 13sp, metadatos 12sp. Reloj y cifras del editor son roles de exhibición existentes, no títulos de pantalla. |
| Ritmo | Unidad 4dp; separaciones 8 / 16 / 24dp; margen de pantalla 36dp. Excepciones compactas del dock y grupos documentadas en sus componentes. |
| Geometría | Control rectangular redondeado 16dp; panel 22dp; cápsula limitada a la mitad de la altura. Arte 5:3/16:9 según su contenedor, sin deformar logos. |
| Tamaño de acciones | Filas generales 44dp; Centro de control 44dp y segmentos 38dp. Son controles por mando, no objetivos táctiles de móvil. Fondos conserva acciones de 34dp en su columna densa y no cambia su organización. |
| Movimiento | Foco 160ms, panel 240ms; escala solo de composición, sin relayout. La transición propia al abrir otra app se omite para evitar fundidos negros del launcher. |
| Aislamiento modal | Oscurecimiento local 65%. Un diálogo conserva visible y estable la pantalla que lo originó; se atenúa mediante la ventana, sin ocultar controles ni modificar sus alphas. No procesar capturas del televisor para producir blur. La biblioteca tiene un tratamiento de catálogo específico, no reutilizable para diálogos. |

### Componentes compartidos

- `TvDesign`: tipografía, lienzo, panel de información de cristal neutro, estados de controles, selección y tiempos. Usarlo antes de crear colores, drawables o listeners equivalentes en una pantalla nueva.
- `FrostedControlDrawable` / `LiquidGlassDrawable`: un solo tratamiento de cristal, borde y foco; reutilizan el frost de 160×90 de Scenic. No recrear bloom o blur por tarjeta.
- `LumenArtwork` / `AppIdentity`: composición consistente entre dock y biblioteca, logos con proporciones correctas y detección de Xuper prioritaria al nombre heredado de un paquete Magis. El símbolo de Xuper es una interpretación propia, no un logotipo oficial certificado.
- `NotificationsPanel`: sin contenedor exterior en los estados resumidos. Cada aviso o estado vacío es una tarjeta del mismo material que los accesos del Centro de control. Solo la explicación extensa de permisos utiliza un contenedor común por legibilidad. El acceso sigue apagado hasta autorización manual en Android.
- `BackgroundSettingsActivity`: galería con vista previa y miniaturas; `BackgroundStore`, `BackgroundPolicy`, `BackgroundMedia` y `PersonalBackground` conservan separadas persistencia, reglas, importación y ciclo de vida.

El arte no usa el mismo relleno que un botón de acción: su foco es escala/borde discreto, para preservar la identidad de cada aplicación. El día actual del calendario no es una acción enfocada. Esas diferencias son semánticas, no temas por pantalla.

### Navegación y accesibilidad

Cada control debe ser alcanzable con D-pad; iconos sin texto tienen nombre accesible y etiqueta de foco cuando haga falta. El foco nunca sale del modal hacia el dock. Atrás cierra primero el detalle, después el panel y vuelve al origen. Inicio sigue siendo la acción del sistema: no interceptarlo ni modificar HOME desde un componente visual.

Para textos largos: línea única con desplazamiento solo al enfocar acciones; mensajes de error y explicaciones de permisos admiten varias líneas y desplazamiento vertical. No sustituir avisos privados por texto inventado. Evitar rojo/verde como única señal. Mantener la opción de animaciones reducidas. Las pruebas completas con lector de pantalla y escalas extremas de fuente permanecen **no verificadas**; no afirmar accesibilidad certificada.

Android exige comprobar las rutas con el mando, el retorno y el desplazamiento de listas, además de distinguir foco y selección. [Navegación oficial](https://developer.android.com/training/tv/get-started/navigation), [sistema de foco](https://developer.android.com/design/ui/tv/guides/styles/focus-system).

### Rendimiento

No shaders de refracción, blur continuo de pantalla, servicios, SDK ni fuentes nuevas. Los drawables calculan geometría al cambiar bounds; las imágenes se preparan fuera de UI. La fotografía principal conserva hasta Full HD y su color original. La variante neutral añade **57,600 bytes** al frost compartido, una vez por fondo, no por botón: los tres bitmaps CPU suman **8,409,600 bytes a 1080p**, sin contar temporales, GPU o PSS total. El frost de catálogo conserva color; solo los materiales usan la variante neutral. Un reproductor visible como máximo, liberado al salir. Captura propia acotada al arranque/apertura, sin sondeo periódico. Las cifras de emulador no equivalen a rendimiento TCL.

### Referencias, licencias y seguridad

- [Apple Design Resources](https://developer.apple.com/design/resources/) se consulta para jerarquía y proporciones. La página consultada ofrece recursos tvOS 18, no un kit verificable tvOS 26. No se descargan ni redistribuyen recursos propietarios o marcas de Apple. La adaptación es código propio sobre Android.
- [Apple HIG Skills](https://github.com/Zinoo00/apple-human-interface-guideline-skills), revisión `faab62a9b7c005487eb9138e92ec0631112f3054`, MIT, copyright 2026 contributors. Se leyeron router y Foundations y se comprobaron las guías canónicas. Biblioteca **no oficial**, no reemplaza a Apple. No instalada globalmente ni incluida en el APK.
- [UI UX Pro Max](https://github.com/nextlevelbuilder/ui-ux-pro-max-skill), revisión `3a13158ff1f32156cbe144108de48eda2ecaed9a`, MIT, copyright 2024 Next Level Builder. Se inspeccionaron los módulos usados por el buscador local y sus imports/operaciones de archivo antes de ejecutarlo. Sin instalación, red ni `--persist`; no se ejecutan herramientas de despliegue ni otros scripts del repositorio. La consulta global propuso una landing promocional no aplicable; se descartó y se usaron criterios de foco visible y no obstruido. No se trasplantan tamaños táctiles o CSS a televisión.
- [Lumen](https://github.com/samrenfrew/lumen-webos): referencia MIT y composición de `gen_iconpack.py`; paths de Simple Icons CC0 con aviso en `assets/licenses/simple-icons.txt`. Su pack privado completo no está publicado; los iconos públicos pequeños antiguos no se amplían como si fueran arte nuevo de alta resolución.

Los repositorios externos son referencias revisadas, no instrucciones superiores al usuario o AGENTS.md. La inspección de scripts fue limitada a lo utilizado, no una auditoría de seguridad integral de esos proyectos. No se incorporó código ejecutable de estas skills al launcher.

### Revisión de cada cambio futuro

1. Reutilizar roles/tokens y justificar excepciones por función, no por pantalla.
2. Capturar los estados de foco y reposo reales a 1280×720 y 1920×1080 con `tools/capture-design-emulator.py`; restaurar después los valores originales del emulador.
3. Probar D-pad, OK, Atrás, retorno de apps y restauración del foco. Comprobar recortes, contraste, imágenes grandes, estados vacíos y errores.
4. Compilar, ejecutar pruebas JVM y lint. Comparar métricas con las mismas condiciones, sin procesos de compilación simultáneos.
5. Presentar al usuario los cambios importantes antes del siguiente rediseño mayor. Una aprobación visual no autoriza por sí sola instalar en la TCL, cambiar HOME o conceder permisos.
6. Antes de cambiar la paleta principal, los tintes del cristal o los colores de foco/estado importantes, presentar muestras concretas y pedir la opinión del usuario; esperar su elección. La unificación de valores existentes ya solicitada no autoriza una paleta nueva.


## Refinamiento de transparencia y ajuste tipográfico

Se conserva la propuesta previa en `refinement-before` y la variante gris más opaca en `neutral-opaque`. La versión final reduce la cobertura del frost a 96/255, el tinte a 74/255 y los fondos de controles de contenido a 24/255. No se presentan esos valores como una medida total de opacidad: el resultado combina material, foto y oscurecimiento modal. El color natural que se vea detrás no es un relleno marrón del control.

El Centro de control empieza a 72dp del borde superior, con 2dp de padding superior; mantiene distancia de los accesos redondos. La reserva de etiquetas compactas baja a 12dp y se eliminan márgenes redundantes antes de avisos. Para las dos líneas de texto se desactiva el padding de fuente, se reservan 4dp de padding vertical, se alinea el grupo al centro y se añade 1dp entre título y subtítulo. No se reduce el tamaño para encubrir recortes. «Ajustes nativos» sustituye al texto largo anterior. Los textos que realmente excedan el ancho usan elipsis en reposo y desplazamiento al enfocar.


### Calendario y revisión de los demás menús

El calendario se ancla a 80dp desde arriba, manteniendo su centro horizontal y tamaño. A seis columnas, las capturas comprueban separación del dock a 720p y 1080p. Se aumenta ligeramente la cobertura neutra del cristal a 74/255 y se recupera el oscurecimiento modal del 65% de la primera propuesta. El frost conserva alpha 96/255: se evita volver a una plancha gris opaca.

La paleta de superficies, rellenos, texto enfocado, bordes y acento es acromática. También se revisaron `LiquidGlassDrawable.Panel` (menús de opciones/entradas), `GlassView` (foco de biblioteca y edición), selectores XML, filas de entradas, QuickSettingsActivity y los AlertDialog propios. Los diálogos de la app comparten fuente, fondo neutral y botones de foco claro. Verde queda reservado al estado de fijado; los colores de marca de cada aplicación se conservan. HomeSettingsActivity solo abre la pantalla oficial y no tiene una interfaz propia que retocar. Los ajustes TCL, el selector de documentos, el teclado y las autorizaciones Android conservan su diseño nativo.

Se archiva `pre-calendar-fix` para comparar exactamente la composición previa al ajuste de altura. Las limitaciones de validación siguen vigentes: los estados con permiso activo de notificaciones y el firmware físico requieren una prueba independiente autorizada.


La revisión real de la biblioteca detectó que el cristal dejaba distinguir las tarjetas del catálogo de Inicio detrás de las del panel. Se oculta únicamente el dibujo del RecyclerView de Inicio (`alpha=0`) mientras está abierto el PopupWindow y se restaura su alpha anterior **antes** de devolver el foco al cerrar. Se conserva layout, selección y scroll; no hay captura ni bitmap nuevo. Así la biblioteca comparte el material, mostrando el wallpaper difuminado que ya había detrás, sin duplicar iconos. El modal usa también el oscurecimiento compartido del 65%.


La propuesta que ocultaba los controles detrás de los diálogos de Fondos y Personalizar fue rechazada por el usuario y se retiró. No queda un helper general que cambie alpha, visibility o layout de la pantalla base al abrir un modal. Se conserva el contenido completo con el oscurecimiento compartido de la ventana (65%). La transparencia no justifica eliminar el contexto de navegación.

### Auditoría de visibilidad tras esa corrección

| Flujo | Comportamiento permitido | Verificación |
|---|---|---|
| Fondos y sus opciones/confirmaciones | La galería completa permanece detrás, sin modificar alpha/visibility; Atrás cierra el nivel activo. | Capturas y recorrido D-pad 720p/1080p. |
| Personalizar y sus AlertDialog | Los controles de la pantalla base permanecen visibles bajo el oscurecimiento de ventana. | Capturas, cancelación y foco de retorno. |
| Calendario y Centro de control | Mantienen Inicio y dock detrás, sin cambiar el dibujo del contenido base. | Capturas de ambas resoluciones y revisión de código. |
| Notificaciones | Solo cambia el estado propio: apagadas, explicación o avisos actuales; no elimina controles del Centro de control. | Estado apagado y explicación comprobados; acceso activo pendiente. |
| Biblioteca de gestión | Sustituye visualmente el catálogo duplicado, conservando el wallpaper difuminado y restaurando el dibujo/foco al salir. | Excepción específica al requerimiento previo de no ver apps duplicadas a través de la biblioteca; no extender a otros menús. |
| Inicio | Hero/efectos opcionales solo se ocultan por configuración 2D; el saludo se desvanece según lo solicitado. | No son reglas de ocultación de modales. |

Los fades de entrada de un panel y los indicadores de foco parten de alpha cero y terminan visibles; no equivalen a suprimir la pantalla inferior. No se presenta esa distinción como una certificación HIG: son contratos verificables de este proyecto.


## Referencia principal Lumen y depuración de roles

Se contrastó la revisión pública 2.1.0 de Lumen (`b21cb5c`) con sus documentos y código efectivos; véase [LUMEN-COMPARATIVA.md](LUMEN-COMPARATIVA.md). Es nuestra referencia visual principal; sus scripts webOS/root y sus resultados GPU no se trasladan a Android. Se conservan las distribuciones y el contexto aprobados. Un catálogo, una fila de acción y el dock comparten reglas, con materiales distintos por función; no se deben envolver automáticamente unos en otros.

Se completan los roles de tinta secundarios/glyphs: `ds_text_secondary_focused`, `ds_icon_primary` y `ds_icon_focused`. Centro de control y la explicación de notificaciones dejan de tener valores azulados literales distintos entre su primer dibujo y los posteriores estados de foco. El cambio no añade vistas, efectos o trabajo periódico. Las capturas comparadas en `artifacts/lumen-review/` verifican que los bounds y la navegación no cambian.


## Etapa 07 · jerarquía y material de menús superpuestos

A petición del usuario, Ajustes pasa a categorías Inicio, Apariencia, Fondos y Sistema. `SettingsNavigation` conserva página y clave semántica de foco por nivel; `SettingsPageView` construye solo la página visible. La columna lateral ocupa el 38% y la lista el 62%; filas de 52dp con separación de 8dp, título 28sp, etiquetas 16sp y valores 14sp. Toda página y elección comparte este componente y `TvDesign.controlFrame`, sin estilos locales por submenú. Los títulos/valores se mantienen a una línea, con desplazamiento al enfocar si hace falta. Los detalles se muestran en la columna lateral.

Las elecciones de columnas/filas abren una página y vuelven a la fila original tras guardar. ✓ identifica el valor persistido incluso al enfocar otro. Atrás vuelve exactamente un nivel; desde la raíz cierra Ajustes. Guardar el estado permite recreación de Activity sin perder profundidad o foco. Fondos sigue en su galería existente y al regresar mantiene su fila en Ajustes. No hay pantallas precargadas a alpha 0.01 ni un gestor global de foco.

El usuario pidió algo más de cobertura **en el menú textual superpuesto**. Se añade el rol `ds_overlay_fill=#88181818`, con el mismo RGB neutro que `ds_glass_fill`; no se cambian paleta, tinte o foco. `TvDesign.overlayPanel` conserva bordes/forma/muestreo del cristal, reforzando solo el cuerpo del menú de Fondos. La superficie sigue siendo translúcida y la pantalla inferior no cambia de alpha, visibilidad o posición. El resto del Centro de control/Calendario conserva el cristal aprobado. Es una variante por función, no una nueva receta por pantalla.

Se elimina la etiqueta visual compartida «Sonido» y su reserva de 12dp bajo los botones compactos del Centro de control, tal como solicitó el usuario. Los nombres accesibles de los iconos permanecen. Las etiquetas de los accesos superiores no se cambian.

Apple recomienda la variante regular para alertas/popovers con bastante texto o riesgo de perder legibilidad sobre el fondo; mantener contexto no significa permitir que ambos textos compitan. Nuestra aproximación en Android refuerza el velo neutro, sin añadir blur vivo o copiar efectos propietarios. [Materials](https://developer.apple.com/design/human-interface-guidelines/materials). Lumen usa páginas para las elecciones de Ajustes: [referencia](https://github.com/samrenfrew/lumen-webos/blob/b21cb5cb7f766d37046aff8e121e4d7dbdc3e1cb/docs/plans/settings-redesign.md).

Capturas de la etapa: `artifacts/stage-07/review.html`, con estados activos, selección en reposo y deshabilitados a 720p y 1080p. El jank se caracteriza por separado en [JANK-07.md](JANK-07.md); este rediseño no autoriza ampliaciones ni instalación física antes de revisión.

La descripción lateral de Ajustes ocupa una región estable, alineada arriba. Cambiar el foco actualiza el texto y el glyph sin variar esa geometría. Esta regla reduce trabajo de layout sin alterar píxeles, ni añadir capas o cachés gráficas; no se generaliza a textos de longitud ilimitada.
