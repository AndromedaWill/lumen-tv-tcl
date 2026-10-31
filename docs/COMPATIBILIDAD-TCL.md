# Compatibilidad TCL C6K: evidencia y límites

Fuente congelada: `cleanyourtv` fe9ad320, [examples/tcl-55c7k-uk/README.md](https://github.com/Gathin23/cleanyourtv/blob/fe9ad320/examples/tcl-55c7k-uk/README.md). También revisado el código de entradas, configuración y servicios. No se ejecutó su receta de instalación.

## Lo que consta del C6K brasileño

G08, Smart TV Pro, G08_4K_GB, UTT2.250416.001 AU02, mercado pt-BR, Ethernet. El informe dice un día de uso y dos reinicios, no validación prolongada. HDMI abrió `com.tcl.tv/com.tcl.player.TVActivity`; TCL Live necesitó `com.tcl.channelplus`, registrado como TV input sin icono launcher. `com.tcl.ttvs` conserva actualizaciones, aunque ese firmware comparte función con tracking. Mantenerlo. Conservar igualmente Game Bar, casting, AirPlay, dreamx, Ginga y servicios desconocidos.

Al retirar el launcher original, algunos botones de apps dejaron de funcionar. El propietario prefirió no activar helper y conservar repetición normal de volumen. La unidad polaca describe HomeKeyService causando un paso por pulsación y restricciones AUTO_START tras reinicio. No adoptar su modificación de appops/accesibilidad.

Ambas describen UI 1920×1080, densidad 320 sobre panel 3840×2160. Es configuración de fábrica observada allí, no permiso para cambiar resolución/densidad aquí.

## Nuestra unidad peruana

Modelo comercial TCL C6K; Android, SoC, fingerprint, red activa, resolución UI y firmware: **sin identificar**. No se presupone G08/AU02 ni componentes brasileños.

Auditoría futura, cuando se autorice conexión, exclusivamente de lectura y serial explícito: getprop selectivo (modelo/producto/device, versión SDK, display de build), wm size/density sin argumentos, dumpsys tv_input, dumpsys audio, dumpsys hdmi_control, resolución del intent HOME, lista de paquetes y activities exportadas de ajustes/TV. Guardar informes completos en backup ignorado por Git; no publicar seriales, SSID, MAC, IP ni cuentas. No ejecutar componentes por exploración.

## Matriz de validación

| Función | Emulador | TCL Perú |
|---|---|---|
| Inicio / D-pad / retorno | Probar en 720p y 1080p | Pendiente aprobación APK |
| Ajustes estándar | Resolver y abrir | Confirmar ajustes TCL y salida con Back |
| Sin HDMI | Verificar mensaje y foco | No prueba de puertos |
| HDMI / CEC / eARC / Game Bar | No validable | Probar cada entrada realmente detectada |
| DRM / Netflix / Prime / Cast / AirPlay | No certifica hardware | Probar sin modificar servicios |
| Repetición de volumen | No sustituye mando | Helper desactivado; comparar salidas de audio |
| Reinicio / Inicio predeterminado | No cambiar Home en esta entrega | Prohibido hasta aprobación y reversión |

## Recuperación sencilla del prototipo

La instalación de prueba futura se abrirá como una aplicación normal, con paquete separado y sin elegirla como Inicio. Para salir, pulsar Inicio: seguirá apareciendo Google TV. También se puede abrir Ajustes → Aplicaciones → TCL Home → Forzar detención. No se necesita habilitar paquetes porque no se deshabilita ninguno. Reiniciar conserva el Inicio original. No distribuir una receta de sustitución de Home hasta conocer firmware, launcher resuelto y procedimiento de recuperación probado.
