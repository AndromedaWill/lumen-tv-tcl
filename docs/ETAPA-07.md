# Etapa 07 · Ajustes funcionales y rendimiento caracterizado

Fecha: 1 de noviembre de 2026 (America/Lima). Candidato de desarrollo para aprobación visual. La TCL real no recibió comandos ni instalación en esta etapa; Google TV sigue siendo la referencia de Inicio de la prueba física anterior.

## Qué se implementó

Los Ajustes del lanzador ahora son una jerarquía nativa para mando, inspirada en la organización de Lumen/tvOS:

- **Inicio:** aplicaciones por fila, filas bajo el dock y nombre local para el saludo.
- **Apariencia:** animaciones, efectos avanzados y restauración de las opciones del tema.
- **Fondos:** entrada a la galería existente, mostrando el modo actual en la fila.
- **Sistema:** acceso a ajustes originales, selector oficial de Inicio y créditos/licencias.

Las elecciones numéricas abren su página, muestran ✓ junto al valor guardado y regresan a su fila al elegir. Mover el foco no cambia la selección. Atrás retrocede un nivel y desde la raíz cierra Ajustes. La galería y los ajustes nativos regresan al origen. Recrear la Activity conserva profundidad y foco. Los valores existentes y las seis imágenes no se migran ni borran; restablecer el tema no borra el nombre o colección.

`SettingsNavigation` guarda solo enums y claves. `SettingsPageView` comparte composición de columnas, filas, tipografía, material y foco para todas las páginas. `LauncherSettingsActivity` controla preferencias y aperturas. Solo se construye la página visible: no hay árboles ocultos precargados. Config se carga al crear la Activity o restablecer el tema; navegar y guardar cambios locales no vuelve a leer el archivo completo. La descripción tiene un espacio estable para reducir relayout al mover el foco.

Se elimina la plantilla XML plana y sus estilos sin consumidores. No se añaden bibliotecas, procesos, servicios ni permisos.

## Pulido solicitado

Se retira la etiqueta visual «Sonido» bajo los accesos compactos y su hueco de 12dp. Los iconos conservan nombres accesibles, D-pad y sus acciones. Las etiquetas superiores de Ajustes/Reorganizar/Entradas permanecen.

El menú textual de Fondos usa una variante algo más cubierta del **mismo cristal neutro** (`ds_overlay_fill=#88181818`, frente al cuerpo general #4A181818). No se cambia el color RGB ni el foco. La fotografía y los controles inferiores siguen visibles y en su posición; no se altera su alpha/visibility. El Centro de control y el calendario conservan la transparencia aprobada. No hay blur vivo, captura periódica ni una caja adicional.

Apple recomienda una variante de material más legible cuando un popover contiene texto sobre un fondo complejo; Lumen evita parte de esos solapamientos usando páginas para elecciones de Ajustes. Es una adaptación de esos criterios, no una reproducción de su código webOS. [Apple Materials](https://developer.apple.com/design/human-interface-guidelines/materials), [organización de Lumen](https://github.com/samrenfrew/lumen-webos/blob/b21cb5cb7f766d37046aff8e121e4d7dbdc3e1cb/docs/plans/settings-redesign.md).

## Evidencia visual

Comparador local: `artifacts/stage-07/review.html`. Incluye categorías, Inicio, elecciones enfocadas/en reposo, Apariencia, deshabilitados, Sistema, créditos, Centro de control y menú del fondo a 720p y 1080p. PNG reales y XML, sin retoques; se identifica el APK de cada colección. Antes de las subpáginas se muestra la lista plana anterior, porque esas páginas aún no existían; no se inventa una pantalla antigua equivalente.

Las capturas anteriores pertenecen a los checkpoints C49/D5F7/10DB indicados; sus diseños se conservaron hasta esta etapa. Las primeras pruebas de la jerarquía en A502 se guardan en `ui-final/`; el candidato con la optimización de descripción en `ui-delivery/`. Las seis páginas comparadas entre ambos candidatos tienen píxeles idénticos a ambas resoluciones: reservar el espacio de descripción no cambia su aspecto (`description-visual-diff.json`). No se reetiquetan imágenes de otro APK. Los iconos de aplicaciones que se ven en el emulador son fixtures, no las apps instaladas en la TCL.

## Resultado de rendimiento

[JANK-07.md](JANK-07.md) documenta hipótesis, datos, descarte de una prueba de capas y límites. La diferencia observada entre EC8011 y 10DB no pudo aislarse de la variación de la sesión gráfica: ambas versiones empeoraron mucho respecto de la ronda anterior. El tiempo dominante está en esperas de presentación/composición; no se presentan esas esperas como tiempo puro GPU ni como latencia del mando.

Se repitió también 10DB frente a A502, con el nuevo sistema de Ajustes pero antes del ajuste de descripción (solo afecta a esa pantalla):

| Orden | APK | Jank | p50 / p95 / p99, ms | PSS, KiB |
|---|---|---:|---|---:|
| A1 | 10DB | 16.00% | 25 / 48 / 53 | 41,795 |
| B1 | A502 | 17.81% | 26 / 48 / 53 | 41,706 |
| B2 | A502 | 17.02% | 25 / 48 / 53 | 41,872 |
| A2 | 10DB | 17.34% | 25 / 48 / 53 | 41,708 |

p95/p99 coinciden, PSS se solapa y la pequeña diferencia de jank no autoriza declarar mejora o ausencia de regresión. Es **caracterización, no una promesa de fluidez final**. El código de la ruta Main no se modificó para los Ajustes jerárquicos; el último ajuste de descripción tampoco afecta a esa ruta. Datos: `final-abba/`.

En el A/B específico de descripción de Ajustes, el candidato final redujo p95 de 22–44ms a 18–19ms y UI p95 de 2.083–2.287ms a 1.041–1.575ms en esa ronda. No se traslada a FPS físicos ni al dock. El contador moderno de jank dio 100% para cambios de foco aislados tanto en la referencia como en el candidato; se conserva junto al legado y a los percentiles, sin ocultar valores desfavorables. Solo hay dos muestras por variante. Una muestra anterior de A502 con p99=300ms se conserva y queda explicada como límite, no descartada.

La muestra de reposo de Ajustes no añadió fotogramas en dos segundos (20 → 20) después del recorrido; no es una medición exhaustiva de CPU. No se mantienen timers de páginas cerradas. Los nuevos tests no recomiendan activar efectos avanzados, notificaciones o servicios para mejorar cifras.

## Verificación y APK

Java17 / SDK34 / wrapper fijado. Build, **155 pruebas JVM sin fallos** (siete nuevas para navegación/restauración), lint **0 errores y 118 advertencias** conservadas. El manifiesto mantiene la misma semántica que 10DB: paquete de prueba `pe.tclhome.prototype`, cinco actividades existentes, seis servicios protegidos declarados, cero receptores estáticos y cuatro permisos heredados. Se cotejó también el manifiesto empaquetado completo resolviendo nombres: solo se renumeraron dos referencias a recursos (`AppTheme` y `sleep_service_name`), sin cambios de componentes, acciones ni permisos. Sin .so que excluya armeabi-v7a; la prueba física de esta revisión sigue pendiente. Firma APK v2 válida en API34, mismo certificado de desarrollo.

APK conservado: `artifacts/stage-07/candidate-39264c9ee740.apk` (6,441,952 bytes).
SHA-256: `39264c9ee740fb47d32e9c60b5fa35c57a4ffda95c36e773bc073ba8d5997523`.

Los tests del sistema jerárquico comprueban valores 4/5/6/7, filas 0/1, guardado/retorno, marca independiente del foco, reconstrucción por cambio de configuración, salto de filas deshabilitadas, cancelación sin activar efectos, retorno de galería/nativos, créditos y ausencia de la etiqueta visual Sonido conservando su nombre accesible. Los 36 escenarios generales de Main/Centro de control pasaron en A502; la optimización posterior solo reserva la altura de la descripción lateral. **El candidato final 39264 pasó los 60 escenarios de Ajustes (30 por resolución), más las aserciones de accesibilidad/material y servicio apagado.** `ui-delivery/1080` y `ui-delivery/720` registran su SHA, resultados y capturas. La salida JSON del verificador usa UTF-8 explícito en Windows.

Comandos reproducibles, **solo emulador explícito**:

```text
powershell -File tools/build-prototype.ps1
python tools/test-settings-emulator.py --serial emulator-5554 --height 1080 --output artifacts/settings-new-1080
python tools/test-settings-emulator.py --serial emulator-5554 --height 720 --output artifacts/settings-new-720
python tools/compare-navigation-emulator.py --serial emulator-5554 --baseline artifacts/lumen-review/tcl-home-10db36445cff.apk --candidate artifacts/stage-07/candidate-39264c9ee740.apk --output artifacts/navigation-new
```

El build no instala. Los tests de UI y comparación verifican qemu, usan el serial explícito, restauran tamaño/densidad del emulador y no eligen HOME ni conceden permisos. Los recorridos de preferencias son para el AVD de fixtures; no ejecutarlos contra datos personales. Archivos de firma, manifiesto y resultados están en la carpeta de artefactos de la etapa.

## Puerta siguiente

Se entrega para aprobar estas pantallas. **No se añadieron alias HDMI, recientes ni otra ampliación**: siguen después de aprobar Ajustes, como pidió el usuario. Los colores importantes se consultarán antes de cambiarlos; aquí solo se reforzó la cobertura neutra solicitada.

La caracterización de jank sigue abierta como P1 para una medición física autorizada. Antes de recomendar una actualización estable: repetir dock/catálogo, retorno y vídeo real en la TCL, confirmar nitidez/mando/proveedor de archivos y medir memoria/frames con condiciones controladas. La sustitución HOME, accesibilidad, limpieza, entradas y volumen global no se combinan con esta prueba. El launcher original y servicios nativos se conservan.
