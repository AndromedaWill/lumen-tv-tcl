# Plan por entregas comprobables

## Entrega 0–1 actual

1. Fijar referencias Git y auditar arquitecturas, permisos, riesgos y diferencias (AUDITORIA.md).
2. Preparar Windows con JDK 17, SDK/build-tools 34, Gradle wrapper verificado y Android TV AVD independiente.
3. Compilar el commit original antes de cambios de producción y conservar APK/log/hash como baseline local.
4. Implementar Inicio sobre navegación existente: composición original, dock, foco y retención; pruebas de D-pad y capturas del emulador, sin apps ficticias en el producto.
5. JVM + lint + compilación release; comparar arranque, PSS, CPU y frames con baseline en mismo AVD.
6. Entregar APK de desarrollo, capturas reales, limitaciones y solicitar aprobación visual. No seguir con otro rediseño mayor hasta respuesta.

## Después de aprobar Inicio

2. Fondos por cinco franjas: política pura con pruebas de límites y medianoche; importador transaccional; modo fijo/horario/vídeo; cancelar trabajo al perder visibilidad. Pruebas de ficheros inválidos, falta de espacio y reloj cambiado.
3. Centro de control: resolver settings públicos y nativos; panel D-pad con retorno de foco; ninguna accesibilidad requerida. Aprobar captura antes de ampliar.
4. HDMI: extracción del código existente, alias persistentes y estados reales. Probar vacío en AVD y hardware solo con permiso.
5. Rendimiento: carga asíncrona con generaciones si medición lo justifica; trim de bitmaps y caché acotada. No afirmar mejoras sin números.
6. TV: inventario de lectura, aprobación APK y reversión, después instalación como app sin sustituir Home; pruebas manuales de streaming, entradas, audio y reinicios. Sustitución de Inicio es otra aprobación.
7. Volumen: solo investigación separada y decisiones A/B/C de VOLUMEN-EXPERIMENTAL.md.

## Puertas de aceptación

Sin crash/ANR; foco visible y sin trampas; ninguna tecla de volumen interceptada; ninguna consulta de red nueva; sin actividades persistentes ocultas. Capturas 1280×720 y 1920×1080 reales, no escaladas para simular pruebas. Registrar cold/warm por varias muestras, gfxinfo tras recorrido idéntico, PSS y CPU tras reposo. Si las muestras son ruidosas, declararlo; el emulador solo sirve para regresiones locales.

No generar commits de APK, SDK o JDK. Mantener hashes y comandos de reproducción. Cada cambio funcional tiene commit separable; revertir por Git sin modificar estado del televisor. El código heredado no se considera automáticamente validado por compilar.

## Objetivo aclarado: Inicio principal, no app secundaria

El candidato final debe ocupar Home y ser la experiencia principal al encender y al salir de apps. La apertura manual solo se usa durante pruebas. Ver INICIO-PRINCIPAL.md: preferir selector oficial/preferencia/rol manteniendo stock habilitado, identificar firmware antes de elegir método y no activar nada sin aprobación. Añadir arranque frío, standby, Home físico y restauración a las condiciones de aceptación. El éxito del comando ADB por sí solo no aprueba el cambio.

## Estado después de la revisión física inicial

El primer APK B859 ya se instaló y abrió con autorización como una aplicación normal; Google TV se comprobó como HOME después. Las observaciones del usuario motivaron el pulido documentado en PULIDO-02 y CONSISTENCIA-03. Ya existen seis fondos locales, una ampliación de las cinco franjas iniciales para usar la colección proporcionada. El editor de horarios y la validación prolongada de memoria/HDMI siguen pendientes.

EC8011 ya se actualizó y abrió con autorización como una app normal, con Google TV verificado como HOME antes y después; véase ACTUALIZACION-TCL-EC8011.md. Sigue probar diseño/fondos/mando y medir rendimiento en la TCL con aprobación de cada lectura. El visor secundario de notificaciones está preparado pero apagado: acceso exclusivamente manual y medición de consumo activo antes de recomendarlo. No conceder permisos por ADB para adelantar esa prueba. Google Home se abrió con autorización; falta verificar el acceso desde el APK ordinario. No agregar nuevos efectos mientras se evalúa esta entrega.

Después: corregir regresiones físicas, aprobar diseño y preparar una prueba independiente del selector oficial HOME_SETTINGS y restauración, conservando Google TV y los servicios nativos. Cualquier limpieza de paquetes continúa siendo otra auditoría reversible y otra aprobación, sin desactivar launcherx/dreamx ni componentes esenciales.


## Hito local 06 y gobernanza visual

Se amplía la galería a modos Fijo (imagen/vídeo), Horario (2–12 imágenes) y Presentación, manteniendo las seis imágenes iniciales. Primero validar edición por mando, importación real por selector, persistencia y liberación de vídeo en emulador. Los cambios importantes de diseño se muestran antes/después bajo [DESIGN-SYSTEM.md](DESIGN-SYSTEM.md); ese documento rige las funciones futuras. No añadir nuevos efectos mientras se revisa esta entrega.

Después de la validación local: aprobación visual y autorización independiente de actualización en la TCL. Allí repetir el caso de vídeo observado por el usuario y el paso dock/catálogo con medidas controladas. Las cifras del emulador no sustituyen esta puerta. Conservar EC8011 y sus datos como referencia. El cambio a Inicio principal, las notificaciones activas, HDMI y cualquier limpieza de paquetes siguen siendo pruebas separadas autorizadas.


## Etapa 07 solicitada: rendimiento y Ajustes primero

1. Caracterizar la diferencia de jank con A–B–B–A y separar UI de esperas de presentación; conservar y revertir los ensayos sin beneficio demostrado. JANK-07.md contiene datos y límites, sin declarar cerrado el caso físico.
2. Implementar categorías/subpáginas compartidas con valores, selección y foco persistidos. Verificar D-pad, Atrás, guardado, reconstrucción y retorno de la galería a ambas resoluciones.
3. Retirar la etiqueta de los botones compactos y reforzar ligeramente el material del menú superpuesto, manteniendo la transparencia de los demás controles y el contexto inferior.
4. Presentar comparativas y resultados para aprobación. Alias locales HDMI, recientes de lanzamientos propios y futuras personalizaciones se mantienen **para la siguiente etapa**, después de aprobar esta. No añadirlas a este candidato.
5. La actualización/medición de la TCL necesita autorización independiente; HOME permanece otra prueba. No usar desactivación de servicios ni cambios globales para mejorar resultados.
