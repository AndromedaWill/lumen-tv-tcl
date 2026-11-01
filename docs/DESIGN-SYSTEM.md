# TCL Home · sistema de diseño v1

Estado: implementación de desarrollo, pendiente de aprobación visual del usuario y de una nueva prueba autorizada en la TCL. Revisión: 1 de noviembre de 2026. A partir de esta versión, las funciones nuevas deben usar estos componentes y justificar cualquier excepción.

## Alcance y evidencia

Android TV con Views/Java, API mínima 26 y objetivo 34; mando D-pad, OK y Atrás. Referencia visual tvOS 26, interacción y ciclo de vida Android. No es una certificación de conformidad con HIG ni una reproducción de SystemUI.

Capturas **reales**, sin retocar: `artifacts/design-system/before/{1080,720}/` y `after/{1080,720}/`. Cada carpeta incluye Inicio, Fondos, Calendario, Personalizar, Centro de control y el estado desactivado de notificaciones. Los pares `focused/resting`, `next/previous` y `controls/preview` cambian el foco; siempre debe existir un foco activo en televisión. `capture.json` registra el emulador y la resolución. Los iconos de Archivos, Cine y Colección pertenecen a fixtures identificados de prueba, no a aplicaciones de la TCL.

## Apple guidance

Apple aconseja arte a pantalla completa, legibilidad a distancia y un foco que destaque suavemente el elemento activo. Es la referencia para jerarquía, escala y movimiento; no prescribe nuestros valores Android. [Designing for tvOS](https://developer.apple.com/design/human-interface-guidelines/designing-for-tvos).

Los materiales deben separar funciones y contenido. Apple reserva Liquid Glass principalmente para controles y navegación y recomienda moderación; los fondos y el contenido textual requieren otro tratamiento. Adoptamos esa distinción sin implementar los efectos ópticos propietarios. [Materials](https://developer.apple.com/design/human-interface-guidelines/materials).

La alineación, los grupos y la revelación progresiva ayudan a entender las relaciones. La composición debe adaptarse al tamaño y al texto. [Layout](https://developer.apple.com/design/human-interface-guidelines/layout). Los tamaños y pesos expresan jerarquía y deben poder leerse desde lejos. [Typography](https://developer.apple.com/design/human-interface-guidelines/typography). El feedback debe ser breve y respetar la reducción de movimiento. [Motion](https://developer.apple.com/design/human-interface-guidelines/motion).

## Inference

La mezcla de un selector gris en Personalizar, botones luminosos en Fondos, calendario translúcido y avisos con otro relleno reduce la coherencia. Conviene compartir roles de superficie y foco, conservando las distribuciones que ya conoce el usuario. Una imagen con muchos detalles requiere una superficie más opaca detrás de información larga. El brillo de foco no debe invadir esquinas ni sustituir una indicación inequívoca de selección.

### Auditoría previa

| Pantalla / evidencia | Hallazgo | Prioridad | Corrección acotada |
|---|---|---|---|
| Inicio, Runtime + Static | El dock ya tiene composición aceptada; iconos instalados cuadrados pueden crear otro marco interior. Hay fixtures en emulador. | P1 | Una sola fábrica de arte para Inicio y biblioteca; vectores nítidos de la referencia Lumen cuando existen, color a toda la tarjeta y logo centrado. No inventar que todos son iconos originales de Lumen. |
| Fondos, Runtime | Título de 30sp frente a 34sp en Personalizar; controles y diálogos fabricados localmente. Vista previa y miniaturas ya aportan la jerarquía solicitada. | P2 | Título y controles compartidos; preservar vista previa grande, modos y colección. |
| Calendario, Runtime + Static | Material fotográfico muy presente; mes, flechas y día actual usan reglas distintas de los controles. | P2 | Panel oscuro de información, misma fuente y foco de flechas; hoy mantiene un indicador diferente del foco. |
| Personalizar, Runtime | Filas de 52dp y título de 34sp; selector gris y margen de 72dp producen otra escala visual. | P2 | Título 28sp, filas 44dp, margen 36dp y controles comunes; marca ✓ para valor guardado, foco independiente. |
| Centro de control, Runtime | El borde superior de los botones invade la zona de accesos redondos; tamaño y separaciones pueden compactarse. | P1 | Sin título, desplazamiento inferior y botones menores; pares de accesos con extremos redondeados; notificaciones debajo. |
| Notificaciones, Runtime | Bloque gris grande y texto redundante «Configurar». | P2 | Superficie oscura compartida; estado compacto «Notificaciones desactivadas». OK revela la explicación y configuración. Sin avisos ficticios. |
| Biblioteca, Runtime + Static | Necesita panel opaco e iconos idénticos a Inicio; el check hace redundante «En Inicio». | P2 | Panel de información y fábrica de arte compartidos, sin repetir el estado en texto. |
| Apertura con vídeo, Runtime + Static | Se cargaba una fotografía ajena como respaldo y se mezclaba con el vídeo; la superficie desaparecía al liberar el decodificador. | P1 | Fotograma del mismo vídeo, captura acotada antes de abrir y cubierta antes de liberar. Sin fundido a una foto de la colección. |

## Project choice

### Tokens y unidades

La fuente de verdad de los valores es `launcher/app/src/main/res/values/design_tokens.xml`. Los números son decisiones de este proyecto, no equivalencias entre puntos de Apple y dp de Android. La fuente es **Inter**, ya licenciada bajo OFL; no se incorpora SF Pro ni SF Symbols.

| Rol | Valor / regla |
|---|---|
| Lienzo | Gradiente oscuro `#182337` → `#0C111C`; el Inicio conserva la fotografía o vídeo elegido. |
| Panel de información | `#1A2230` → `#0D131E`, opaco, borde tenue. Calendario, biblioteca, avisos y diálogos propios. |
| Controles sobre foto | Cristal moderado con frost ya calculado de la foto; no desenfoque en vivo por botón. |
| Texto | Principal `#F4F6FC`, secundario `#BAC6D8`, enfocado `#182133`. |
| Foco de acción | Relleno `#EDF3FC`, texto oscuro; elevación discreta donde ya existe. Nunca un halo blanco exterior. |
| Selección persistente | `#283F58` y marca ✓; no confundir el valor guardado con el control enfocado. |
| Escala tipográfica | Título 28sp, sección 20sp, cuerpo 16sp, control 14sp, compacto 13sp, metadatos 12sp. Reloj y cifras del editor son roles de exhibición existentes, no títulos de pantalla. |
| Ritmo | Unidad 4dp; separaciones 8 / 16 / 24dp; margen de pantalla 36dp. Excepciones compactas del dock y grupos documentadas en sus componentes. |
| Geometría | Control rectangular redondeado 16dp; panel 22dp; cápsula limitada a la mitad de la altura. Arte 5:3/16:9 según su contenedor, sin deformar logos. |
| Tamaño de acciones | Filas generales 44dp; Centro de control 44dp y segmentos 38dp. Son controles por mando, no objetivos táctiles de móvil. Fondos conserva acciones de 34dp en su columna densa y no cambia su organización. |
| Movimiento | Foco 160ms, panel 240ms; escala solo de composición, sin relayout. La transición propia al abrir otra app se omite para evitar fundidos negros del launcher. |
| Aislamiento modal | Oscurecimiento 65%; contenido informativo opaco. No procesar capturas del televisor para producir blur. |

### Componentes compartidos

- `TvDesign`: tipografía, lienzo, panel de información, estados de controles, selección y tiempos. Usarlo antes de crear colores, drawables o listeners equivalentes en una pantalla nueva.
- `FrostedControlDrawable` / `LiquidGlassDrawable`: un solo tratamiento de cristal, borde y foco; reutilizan el frost de 160×90 de Scenic. No recrear bloom o blur por tarjeta.
- `LumenArtwork` / `AppIdentity`: composición consistente entre dock y biblioteca, logos con proporciones correctas y detección de Xuper prioritaria al nombre heredado de un paquete Magis. El símbolo de Xuper es una interpretación propia, no un logotipo oficial certificado.
- `NotificationsPanel`: contenido compacto dentro del Centro de control; permiso y contenido se revelan al pulsar. El acceso sigue apagado hasta autorización manual en Android.
- `BackgroundSettingsActivity`: galería con vista previa y miniaturas; `BackgroundStore`, `BackgroundPolicy`, `BackgroundMedia` y `PersonalBackground` conservan separadas persistencia, reglas, importación y ciclo de vida.

El arte no usa el mismo relleno que un botón de acción: su foco es escala/borde discreto, para preservar la identidad de cada aplicación. El día actual del calendario no es una acción enfocada. Esas diferencias son semánticas, no temas por pantalla.

### Navegación y accesibilidad

Cada control debe ser alcanzable con D-pad; iconos sin texto tienen nombre accesible y etiqueta de foco cuando haga falta. El foco nunca sale del modal hacia el dock. Atrás cierra primero el detalle, después el panel y vuelve al origen. Inicio sigue siendo la acción del sistema: no interceptarlo ni modificar HOME desde un componente visual.

Para textos largos: línea única con desplazamiento solo al enfocar acciones; mensajes de error y explicaciones de permisos admiten varias líneas y desplazamiento vertical. No sustituir avisos privados por texto inventado. Evitar rojo/verde como única señal. Mantener la opción de animaciones reducidas. Las pruebas completas con lector de pantalla y escalas extremas de fuente permanecen **no verificadas**; no afirmar accesibilidad certificada.

Android exige comprobar las rutas con el mando, el retorno y el desplazamiento de listas, además de distinguir foco y selección. [Navegación oficial](https://developer.android.com/training/tv/get-started/navigation), [sistema de foco](https://developer.android.com/design/ui/tv/guides/styles/focus-system).

### Rendimiento

No shaders de refracción, blur continuo de pantalla, servicios, SDK ni fuentes nuevas. Los drawables calculan geometría al cambiar bounds; las imágenes se preparan fuera del hilo principal. La fotografía principal conserva la resolución adecuada de la UI hasta Full HD; el frost pequeño no sustituye su detalle. Un reproductor de vídeo visible como máximo y liberación al salir de Inicio. Captura propia acotada al arranque/apertura, sin sondeo periódico. Toda cifra de emulador debe identificarse como tal; no equivale al rendimiento de la TCL.

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
