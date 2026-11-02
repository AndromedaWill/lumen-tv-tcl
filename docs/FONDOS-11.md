# Continuidad y navegaciÃ³n de fondos Â· 02/11/2026

Alcance: tres defectos reportados con 5a0247462e4b. Se conserva el diseÃ±o aprobado y la fotografÃ­a principal a resoluciÃ³n de interfaz. Sin bibliotecas ni permisos nuevos.

## Cambios

- Fondo fijo: el velo desenfocado de 160Ã—90 preparado previamente se dibuja en una View independiente. La transiciÃ³n usa alpha de composiciÃ³n; deja de invalidar el drawable raÃ­z durante cada fotograma del desplazamiento. No se vuelve a decodificar la fotografÃ­a al bajar.
- VÃ­deo: el estado dock/grid se transmite tambiÃ©n a PersonalBackground y al comienzo del desplazamiento. AerialBackdrop ahora muestra un fotograma del mismo vÃ­deo desenfocado en un worker; lo renueva al entrar en la cuadrÃ­cula y al reiniciar la reproducciÃ³n. No hay blur por fotograma, sondeo ni reproducciÃ³n fuera de Inicio. Si no hay una captura vÃ¡lida se conserva el sombreado existente.
- Apertura: PixelCopy acepta resultados solamente de la generaciÃ³n activa. Una respuesta posterior a pause/reinicio/cierre ya no reemplaza el fotograma vÃ¡lido. Antes de abrir se solicita una captura mientras el reproductor aÃºn estÃ¡ disponible; se despacha al completarla o a los 48 ms, lo que ocurra primero. Este lÃ­mite es un compromiso explÃ­cito entre continuidad y latencia, no una promesa de apertura instantÃ¡nea. Al pausar se libera el reproductor y se conserva la imagen; no se prolonga el decoder en segundo plano.
- Las transiciones interrumpidas se estabilizan al ocultar/reanudar. Se invalidan trabajos de blur al salir y cerrar.

## VerificaciÃ³n

Evidencias brutas, firma, manifiesto, capturas e instrumentaciÃ³n en artifacts/backdrop-11 (ignorado). El test de generaciones cubre respuestas tardÃ­as despuÃ©s de pause, resume y cierre. La instrumentaciÃ³n usa Ãºnicamente un vÃ­deo propio y una app fixture en un emulador verificado. No utiliza notificaciones, accesibilidad ni cambia HOME.

El microcorte observado en la TCL todavÃ­a necesita comparaciÃ³n con el mando en hardware. Las mediciones del emulador no representan su GPU ni demuestran que esta sea la Ãºnica causa. Africa Night no estÃ¡ en el emulador: comprobar la correcciÃ³n del parpadeo en ese vÃ­deo concreto corresponde a la prueba fÃ­sica del propietario. Las transiciones propias de la app destino y del sistema quedan fuera del control del launcher.

## Resultado de esta entrega

APK 847bd18f57c4294f6bfdc56a3e6c3841c1c9409307b76bb7c25ee16e4d7de6f3, 6.465.292 bytes. Firma v2 verificada con el mismo certificado b80940487f3da13532040f22ed5fe2b9a6c370377ab430110a1489d3fb7617f6. Android API 26–34, Java/recursos sin .so; compatible con la ABI armv7 de la TCL. Sin cambios del manifiesto principal ni concesiones automáticas. Compilación release/debug, 171 pruebas JVM (0 fallos/errores) y lintRelease sin errores; registros build-ready.log y signature-ready.txt.

La instrumentación de una revisión previa pasó a 1080p: fundido de foto, estado de blur al bajar y liberación/reanudación del decoder en tres ciclos (final-visual/1080). La revisión visual de esa captura detectó que aún se veía el vídeo nítido: se añadió setWillNotDraw(false) explícito y una espera de composición en la captura de prueba. La repetición del APK final y la validación completa a 720p quedaron interrumpidas porque el proceso del emulador desapareció, también en recorridos con la APK base. No se declara aprobación visual final ni se atribuye con certeza el cierre a la memoria del host. El equipo mostró menos de 1 GiB disponible en varias muestras. Las capturas anteriores se conservan como evidencia de diagnóstico, no como resultado final aprobado.

Los ensayos de rendimiento quedaron afectados por interrupciones y cambios del entorno. NO hay una mejora de jank cuantificada válida, ni comparación con la TCL base. Se descartó la propuesta de una capa adicional para foto. El ajuste final de 100+180 ms requiere evaluación en la TCL. La actualización de prueba está autorizada explícitamente por el propietario; no autoriza cambiar HOME, permisos, servicios o paquetes del fabricante.
