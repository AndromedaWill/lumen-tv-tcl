# Pulido después de la primera prueba TCL

Documento histórico del candidato 226AA9. La revisión posterior de iconos, biblioteca, Centro de control y notificaciones opcionales se documenta en [CONSISTENCIA-03.md](CONSISTENCIA-03.md).

Trabajo local del 31 de octubre de 2026 (Lima). El APK B859 sigue siendo la única versión instalada en la TCL. Las modificaciones de este documento se prueban en `emulator-5554`; requieren aprobación específica para actualizar la TV. No se han cambiado HOME, servicios de accesibilidad, entradas, audio ni paquetes del fabricante.

## Revisión y correcciones

| Observación | Cambio |
| --- | --- |
| Banners antiguos de YouTube/Google/Xuper | Preferir el icono de la aplicación instalada. No descargar marcas de Internet ni sustituir recursos del fabricante. Si el icono que trae la app es antiguo, no se inventa uno nuevo. |
| Texto repetido dentro de las tarjetas | Para estos iconos, solo el símbolo centrado, con tamaño interior de 64 dp frente a 48 dp. La tarjeta conserva su tamaño; el nombre de foco se mantiene fuera. |
| Reflejos cortados en esquinas | El material usa el radio real de la tarjeta, recalculado al cambiar su tamaño; ShineView también limita el brillo con un Path redondeado propio. |
| Indicador de reorganización desplazado | Calcular desde las posiciones de layout y aplicar una sola vez escala/elevación, sin duplicar transformaciones animadas. Se retira el Toast sobre el dock. |
| Acciones superiores sin nombre | Etiqueta discreta debajo del botón enfocado, también en modo reducido y con animaciones desactivadas. |
| Reloj tosco y fecha demasiado separada | Reducir altura reservada de 64 a 52 dp; foco suave con borde fino y fondo tenue, sin bloque blanco. |
| Calendario solicitado | Panel local sin permisos de calendario: flechas enfocables junto al mes, OK cambia el mes y Atrás cierra. Sin instrucciones inferiores. Material difuminado compartido con el Centro de control. |
| Saludo | Saludo según hora local junto al reloj; nombre local editable, inicial Will. Visible 4.2 segundos, desvanecimiento 0.5 segundos; sin repetición durante los siguientes 30 minutos en el proceso. Sin animación si está desactivada. |
| Biblioteca transparente | Panel opaco con el contenido protegido de la imagen de detrás; se conserva el oscurecimiento exterior. |
| Centro de control redundante | Quitar TCL Home · Accesos rápidos; subir filas eliminando 24 dp del subtítulo y reduciendo sus separaciones. |
| Inglés en Personalizar | Completar las traducciones españolas de todas las cadenas heredadas de values/strings.xml. |
| Ajustes completos poco accesibles | Añadir Ajustes del lanzador al principio de Personalizar; el submenú ahora permite desplazamiento sin crecer sobre el dock. |
| Primera imagen del tema en UI | WallpaperPlayer ya inicia la primera decodificación en el trabajador, igual que las posteriores. |
| Recarga al volver de cada app | Consultar el cursor público getChangedPackages de Android 26+ y reutilizar catálogo/iconos cuando no cambian. Mantener reconstrucción tras cambios, reconstrucción de proceso, ajustes e Inicio explícito. Si la consulta falla, usar la recarga conservadora. |

La revisión incluye los componentes modificados, el manifiesto, el ciclo visible de los fondos, las llamadas de lanzamiento y las rutas de configuración. No equivale a garantizar que todo el firmware o todas las aplicaciones instaladas carezcan de defectos.

## Fondos personales y conservación de aéreos

Se empaquetan los seis JPEG originales 2752×1536, verificados byte por byte, desde `.dev/personal-assets/personal/`. El directorio queda ignorado y los originales de Documentos no se modifican. `tools/build-prototype.ps1 -WallpaperSource <carpeta>` prepara las seis entradas; sin colección local, la imagen Hercules sigue siendo respaldo. No distribuir públicamente las imágenes personales sin revisar sus derechos.

El modo inicial es por hora local: 1 alba 05:00–07:00; 2 amanecer 07:00–09:00; 3 día 09:00–16:00; 4 atardecer 16:00–18:30; 5 anochecer 18:30–20:30; 6 noche 20:30–05:00. Se divide la franja original de amanecer para incorporar las seis variantes. En esta iteración los límites son fijos: el editor de horas queda pendiente. Se revalida al reanudar, en el tick visible del reloj y al cambiar hora/zona; sin temporizador ni receptor de hora activo cuando Inicio está pausado. El cambio por tick puede demorarse hasta un minuto respecto al límite.

La imagen principal conserva la resolución de la interfaz hasta el límite de su fuente. Solo la copia para desenfoque se reduce a 160×90. ARGB_8888; trabajo fuera de UI; transición de 450 ms desactivable y también suprimida por modo reducido/animaciones desactivadas. Una fuente pequeña para vidrio se comparte entre los controles. No se decodifican las seis imágenes a la vez. La transición retiene temporalmente los fondos anterior y nuevo, además de los recursos temporales de preparación: medir PSS/GPU reales, no sumar solo los archivos JPEG.

Se puede elegir imagen fija de la colección o conservar los fondos del tema y aéreos. No se borran descargas existentes. El crédito de Hercules pasa a Ajustes/licencias incluidas, sin nombre sobre el paisaje. AerialBackdrop muestra el respaldo estático del launcher mientras espera el vídeo, en lugar de una cubierta de color negro, y libera MediaPlayer al salir de Inicio. La corrección debe validarse con los vídeos de la TCL: el emulador no sustituye esa prueba. Cada aplicación controla su propia pantalla; el launcher no puede imponerle su fondo a una app opaca.

## Importación y limitación real de Android TV

BackgroundSettingsActivity ofrece selección local/importación por variante. ImagePickerIntent comprueba ACTION_OPEN_DOCUMENT y, como alternativa, ACTION_GET_CONTENT; descarta componentes no exportados, no accesibles y el sustituto vacío `com.android.tv.frameworkpackagestubs`. No se añaden permisos de almacenamiento ni SDK externos.

El emulador resuelve OPEN_DOCUMENT al DocumentsStub, pero la alternativa GET_CONTENT sí encuentra el Photo Picker del módulo multimedia de Google: se verificó su apertura y una importación real con retorno RESULT_OK. El inventario ya autorizado de la TCL también contiene TvFrameworkPackageStubs, sin un DocumentsUI identificado; la resolución del proveedor alternativo debe validarse allí. Cuando no hay uno, la pantalla lo explica y conserva la colección integrada. El usuario puede seleccionar los seis fondos integrados sin proveedor externo.

La copia de una URI válida se realiza en un trabajador hacia un temporal privado. Se limita a 24 MiB, JPEG/PNG/WebP horizontal, de al menos 640×360 y como máximo 8192 por eje, y se verifica que se puedan decodificar píxeles con una miniatura acotada. Solo después se reemplaza la copia interna de esa variante. Los originales no se tocan; no se necesita conservar una concesión de URI después de copiar. Error/cancelación conservan la imagen anterior. La ruta de éxito se comprobó en el Photo Picker real del emulador. Faltan importaciones con proveedores del firmware TCL, formatos adicionales y ensayos de falta de almacenamiento.

## Límites explícitos

- El nombre del perfil de Google no se obtiene automáticamente: AccountManager limita qué cuentas ve cada app y no garantiza el nombre público del perfil. Esta versión usa un nombre local editable sin solicitar acceso a cuentas. [API oficial](https://developer.android.com/reference/android/accounts/AccountManager).
- No se reemplaza el volumen global. AudioService de Android 14 protege la sustitución del controlador con permisos del sistema; un APK ordinario no puede prometerla. No hay interceptación de teclas, overlay ni accesibilidad nueva. [Código oficial](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android14-release/services/core/java/com/android/server/audio/AudioService.java).
- La consulta de cambios de paquetes evita trabajo redundante del launcher; no precarga Netflix/YouTube ni altera su arranque. [Contrato de PackageManager](https://developer.android.com/reference/android/content/pm/PackageManager#getChangedPackages(int)).
- No se promete mejora porcentual en la TCL hasta medir esta versión con permiso de actualización. Las cifras anteriores de la TV pertenecen al APK B859.

## Validación

Compilación Java 17/SDK 34, firma, pruebas JVM, lint, capturas y mediciones brutas se conservan bajo `artifacts/polish-02/`. Las capturas proceden de ejecución real del emulador; no son maquetas ni capturas de esta versión en TCL. Resultado final: 133 pruebas JVM sin fallos y lint sin errores (94 advertencias y 131 informaciones, incluidas cadenas españolas específicas aún no externalizadas y advertencias heredadas). Firma APK v2 válida para API 34 con el mismo certificado de desarrollo del APK B859. Cinco actividades, cinco servicios heredados, ningún receptor estático y un proveedor protegido; sin nuevos permisos ni bibliotecas nativas.

APK congelado: `artifacts/polish-02/tcl-home-polish-226aa9207074.apk`, 6,418,578 bytes. SHA-256: `226aa9207074b8ede1a57014509a8dd7ff5f5708aa194232fb2cbb0e507a7d38`. El aumento respecto al primer APK incluye los seis JPEG originales, no una nueva biblioteca de ejecución. Manifiesto/firma/metadatos: `manifest-final.txt`, `signature-final.txt`, `candidate-final.json`.

Navegación/foco: 21 comprobaciones correctas en `smoke-optimized/results.json` (abrir/salir, retorno de foco, reconstrucción tras pérdida de proceso, cuadrícula/biblioteca, Centro de control, ajustes nativos AOSP). Se endureció el arranque del script para no aceptar la pantalla de Inicio del sistema como un falso positivo en un borde de navegación. El primer intento con la app no situada delante se conserva en el registro de desarrollo; se repitió con arranque limpio y comprobación explícita de primer plano.

En `catalog-reuse.json`: una carga inicial del catálogo, todavía una después de seis ciclos sin cambios, y dos después de actualizar una app de prueba mientras el launcher estaba parado. Esto demuestra el trabajo evitado y la invalidación, no una mejora porcentual del arranque de otras apps. Calendario, importación real, restauración de colección y capturas de ambos tamaños se documentan en `final/`.

La prueba siguiente sigue siendo una actualización de nuestra app como aplicación normal. Google TV se conserva como Inicio. Una futura elección de HOME_SETTINGS y una futura lista de paquetes prescindibles son trabajos separados con restauración y aprobación de cada cambio; no se han ejecutado.


### Comprobaciones finales de pantalla

La ejecución final del APK 226AA9 conserva capturas reales de Inicio, reloj, calendario con flechas, Centro de control sin subtítulo, cuadrícula, biblioteca opaca y ajustes. En 1280×720/densidad 213 y 1920×1080/densidad 320 se comprobó que el calendario y los controles caben en pantalla. El emulador quedó restaurado a 1920×1080/320. Resultado y huella: `artifacts/polish-02/final/checks.json`.

Durante el primer cambio de resolución, el Inicio del sistema del emulador tomó el primer plano; esa captura no se considera una ejecución del prototipo. Se repitió esperando a que terminase la configuración y verificando el paquete visible antes de capturar. Las imágenes finales home-720.png y calendar-720.png sí corresponden al launcher. No se aplicó ningún cambio de resolución/densidad a la TCL.

La selección alternativa abrió realmente `com.google.android.providers.media.module` (Photo Picker), se importó una imagen y se recibió RESULT_OK en los ajustes. Después se restauraron las copias de la colección desde nuestra pantalla y se eligió Día. La detección del stub evita lanzar el componente vacío; la existencia de ese stub no implica que GET_CONTENT también esté ausente.
