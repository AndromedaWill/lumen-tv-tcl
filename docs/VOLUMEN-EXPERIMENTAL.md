# Volumen: investigación aislada, desactivada

A. Indicador dentro de Inicio: viable como visualización del volumen que AudioManager realmente expone, sin interceptar teclas. Porcentaje de STREAM_MUSIC no garantiza nivel real de barra de sonido o CEC; etiquetar ruta/estado desconocido y validar mute.

B. Overlay sobre otras aplicaciones: requiere permiso especial y comportamiento aceptado por firmware; no habilitado ni solicitado. Puede duplicar el panel original. No añadir servicio permanente para esta entrega.

C. Sustitución global: un launcher ordinario no tiene una API pública para reemplazar SystemUI o el panel de volumen OEM. No prometerlo; no ocultar SystemUI ni modificar políticas de audio.

Evidencia upstream: G08/C6K brasileño y C7K polaco; servicio de filtrado de teclas puede eliminar la aceleración al mantener volumen. `VolumeKeys` y consume_volume_keys son soluciones para otros comportamientos de firmware, no evidencia de corrección universal. ADB input keyevent no reproduce el recorrido del mando por AccessibilityService.

Protocolo futuro: comparar mando físico sin helper en altavoz TV, Bluetooth, ARC/eARC y dispositivo CEC; registrar subida/bajada mantenida, mute, cambio de salida y panel. Cualquier doble indicador, pérdida de repetición o anomalía de audio cancela el experimento. No tocar ajustes Dolby, salida digital ni servicios TCL. Esta fase necesita identificación real y autorización específica antes de cualquier servicio.

## Comprobación de APIs (31/10/2026)

AudioManager público permite consultar/ajustar índices de streams cuando la política no es fija; FLAG_SHOW_UI pide al sistema mostrar su panel, no permite diseñarlo ni reemplazarlo. isVolumeFixed puede impedir los cambios. No debe inferirse el volumen físico de una barra CEC desde el porcentaje de STREAM_MUSIC.

Fuentes primarias: https://developer.android.com/reference/android/media/AudioManager.html y AudioService AOSP (android14-release): https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android14-release/services/core/java/com/android/server/audio/AudioService.java . setVolumeController está protegido por STATUS_BAR_SERVICE y su implementación indica Only SystemUI can. No se intenta llamar a esa API oculta, conceder permisos de firma ni alterar servicios.

Resultado para el usuario: con un APK normal no se puede prometer sustituir globalmente la barra lateral TCL bajo las restricciones acordadas. Un indicador propio dentro de Inicio sí es viable como función experimental separada, pero no se presenta como reemplazo global ni se añade ahora una segunda barra que duplique la original. En esta entrega no hay cambios de volumen, nuevos permisos de audio, overlays ni accesibilidad activada.

## Evidencia de la unidad peruana

La lectura autorizada de AudioService confirma salida speaker, STREAM_MUSIC 0–100, mUseFixedVolume=false y mIsSingleVolume=true. mExtVolumeController=null; CEC de volumen activo. No se envió ningún ajuste. Esto orienta una futura prueba local de indicador/control con APIs públicas; no concede el permiso privilegiado del controlador global ni certifica ARC/eARC/Bluetooth o repetición del mando. Identificación inicial completada; helpers, overlays e intercepción siguen apagados. Ver DIAGNOSTICO-TCL-PERU.md.
