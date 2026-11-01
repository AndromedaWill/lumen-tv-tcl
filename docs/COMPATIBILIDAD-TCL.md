# Compatibilidad TCL C6K: evidencia y límites

Fuente congelada: `cleanyourtv` fe9ad320, [examples/tcl-55c7k-uk/README.md](https://github.com/Gathin23/cleanyourtv/blob/fe9ad320/examples/tcl-55c7k-uk/README.md). También revisado el código de entradas, configuración y servicios. No se ejecutó su receta de instalación.

## Lo que consta del C6K brasileño

G08, Smart TV Pro, G08_4K_GB, UTT2.250416.001 AU02, mercado pt-BR, Ethernet. El informe dice un día de uso y dos reinicios, no validación prolongada. HDMI abrió `com.tcl.tv/com.tcl.player.TVActivity`; TCL Live necesitó `com.tcl.channelplus`, registrado como TV input sin icono launcher. `com.tcl.ttvs` conserva actualizaciones, aunque ese firmware comparte función con tracking. Mantenerlo. Conservar igualmente Game Bar, casting, AirPlay, dreamx, Ginga y servicios desconocidos.

Al retirar el launcher original, algunos botones de apps dejaron de funcionar. El propietario prefirió no activar helper y conservar repetición normal de volumen. La unidad polaca describe HomeKeyService causando un paso por pulsación y restricciones AUTO_START tras reinicio. No adoptar su modificación de appops/accesibilidad.

Ambas describen UI 1920×1080, densidad 320 sobre panel 3840×2160. Es configuración de fábrica observada allí, no permiso para cambiar resolución/densidad aquí.

## Nuestra unidad peruana

Auditoría de solo lectura autorizada el 31/10/2026: G08/G08_4K_GB, Android 14/API 34, MediaTek MT9689, build UTT2.250416.001 AU02, apps de 32 bits. Panel 3840×2160 con override de interfaz 1920×1080 y densidad 320 ya existentes. Usuario 0, Google TV launcherx como Home y titular del rol HOME. Se detecta un manejador oficial HOME_SETTINGS. Ver [DIAGNOSTICO-TCL-PERU.md](DIAGNOSTICO-TCL-PERU.md). Estos datos no certifican todavía sustitución de Home ni comportamiento tras encendido.

Auditoría inicial registrada en DIAGNOSTICO-TCL-PERU.md. Cualquier lectura adicional necesita aprobación individual y destino explícito; no ejecutar un script en bloque bajo esa condición. Catálogo de lecturas posibles: getprop selectivo (modelo/producto/device, versión SDK, display de build), wm size/density sin argumentos, dumpsys tv_input, dumpsys audio, dumpsys hdmi_control, resolución del intent HOME, lista de paquetes y activities exportadas de ajustes/TV. Guardar informes completos en backup ignorado por Git; no publicar seriales, SSID, MAC, IP ni cuentas. No ejecutar componentes por exploración.

## Matriz de validación

| Función | Emulador | TCL Perú |
|---|---|---|
| Inicio / D-pad / retorno | Prototipo comprobado en 720p y 1080p | EC8011 actualizado y abierto con autorización como app; Google TV sigue HOME, nueva revisión del mando pendiente |
| Ajustes estándar | Resolver y abrir | Confirmar ajustes TCL y salida con Back |
| Sin HDMI | Verificar mensaje y foco | No prueba de puertos |
| HDMI / CEC / eARC / Game Bar | No validable | Probar cada entrada realmente detectada |
| DRM / Netflix / Prime / Cast / AirPlay | No certifica hardware | Probar sin modificar servicios |
| Repetición de volumen | No sustituye mando | Helper desactivado; comparar salidas de audio |
| Reinicio / Inicio predeterminado | No cambiar Home en esta entrega | Prohibido hasta aprobación y reversión |
| Google Home | No existe el panel de Google en el AVD | Acción exportada identificada y abierta una vez con autorización; falta acceso desde el APK actualizado |
| Notificaciones | Visor opcional, permiso manual sin conceder | Panel nativo protegido por STATUS_BAR_SERVICE; visor propio apagado, sin acceso concedido |

## Recuperación sencilla del prototipo

La primera instalación de prueba se abrió como una aplicación normal, con paquete separado y sin elegirla como Inicio. Para salir, pulsar Inicio: seguirá apareciendo Google TV. También se puede abrir Ajustes → Aplicaciones → TCL Home → Forzar detención. No se necesita habilitar paquetes porque no se deshabilita ninguno. Reiniciar conserva el Inicio original. La siguiente actualización requiere otra autorización. No ejecutar la sustitución de Home hasta aprobar la versión y comprobar la restauración.

Google Home depende del paquete `com.google.android.apps.tv.dreamx`; Google TV y su panel protegido de avisos dependen de `com.google.android.apps.tv.launcherx`. Ambos se conservan. La preparación del visor propio no concede acceso a notificaciones ni cambia accesibilidad. Evidencia y límites: [CONSISTENCIA-03.md](CONSISTENCIA-03.md).
