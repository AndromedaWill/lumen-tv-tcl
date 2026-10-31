# Volumen: investigación aislada, desactivada

A. Indicador dentro de Inicio: viable como visualización del volumen que AudioManager realmente expone, sin interceptar teclas. Porcentaje de STREAM_MUSIC no garantiza nivel real de barra de sonido o CEC; etiquetar ruta/estado desconocido y validar mute.

B. Overlay sobre otras aplicaciones: requiere permiso especial y comportamiento aceptado por firmware; no habilitado ni solicitado. Puede duplicar el panel original. No añadir servicio permanente para esta entrega.

C. Sustitución global: un launcher ordinario no tiene una API pública para reemplazar SystemUI o el panel de volumen OEM. No prometerlo; no ocultar SystemUI ni modificar políticas de audio.

Evidencia upstream: G08/C6K brasileño y C7K polaco; servicio de filtrado de teclas puede eliminar la aceleración al mantener volumen. `VolumeKeys` y consume_volume_keys son soluciones para otros comportamientos de firmware, no evidencia de corrección universal. ADB input keyevent no reproduce el recorrido del mando por AccessibilityService.

Protocolo futuro: comparar mando físico sin helper en altavoz TV, Bluetooth, ARC/eARC y dispositivo CEC; registrar subida/bajada mantenida, mute, cambio de salida y panel. Cualquier doble indicador, pérdida de repetición o anomalía de audio cancela el experimento. No tocar ajustes Dolby, salida digital ni servicios TCL. Esta fase necesita identificación real y autorización específica antes de cualquier servicio.
