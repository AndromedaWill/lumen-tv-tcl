# Actualización autorizada de la TCL: EC8011

Se actualizó únicamente `pe.tclhome.prototype` y se abrió como aplicación normal. El usuario autorizó individualmente la consulta del paquete, la consulta HOME antes y después, la actualización del APK exacto y su apertura tras Success. No se cambió HOME ni se habilitaron accesibilidad o acceso a notificaciones. No se concedieron permisos mediante ADB ni se modificaron paquetes del fabricante, entradas, audio o pantalla.

## Resultados confirmados

| Comprobación | Resultado |
| --- | --- |
| Destino | Serial TLS de la TCL ya conocida, producto G08_4K_GB, transporte 1, verificado en el inventario ADB local |
| Paquete previo | `pm path --user 0 pe.tclhome.prototype` devolvió su base.apk |
| HOME previo | `com.google.android.apps.tv.launcherx/.home.HomeActivity`, prioridad 2 |
| Actualización | Success |
| Apertura explícita | Status ok, COLD, MainActivity del prototipo |
| Lanzamiento informado | TotalTime 2894 ms; WaitTime 2937 ms |
| HOME posterior | Mismo Google TV y prioridad 2 |

El arranque informado por `am start -W` no verifica calidad visual, latencia del mando o estabilidad posterior. No se presenta como mejora o regresión frente a los 2224 ms de B859: son muestras individuales sin condiciones equivalentes controladas. Revisión personal y nuevas lecturas de PSS/gfxinfo/captura pendientes de respuesta y aprobación específica.

## Candidato y evidencia

APK: `artifacts/control-pairs-05/tcl-home-ec80115ddb25.apk`, 6,424,002 bytes. SHA-256: `ec80115ddb254fb89c526cb0e0c8cb86c47d7798dda796fee2e6dddc73a084f4`. Firma APK v2 válida para API 34, mismo certificado de desarrollo que B859. Checkpoint de código `fc13610f`, precedido por `a899f4e5`. Las pruebas previas son de emulador y no se atribuyen al hardware TCL: [CONSISTENCIA-03.md](CONSISTENCIA-03.md).

Tras comprobar el hash local, el destino autorizado y las condiciones previas, se ejecutaron:

```text
adb -t 1 shell pm path --user 0 pe.tclhome.prototype
adb -t 1 shell cmd package resolve-activity --brief --user 0 -a android.intent.action.MAIN -c android.intent.category.HOME
adb -t 1 install -r --user 0 "C:\Users\milmil\Documents\ChatGPT\Apple tv en andriod tv\artifacts\control-pairs-05\tcl-home-ec80115ddb25.apk"
adb -t 1 shell am start -W -n pe.tclhome.prototype/com.gathin.tvhome.MainActivity
adb -t 1 shell cmd package resolve-activity --brief --user 0 -a android.intent.action.MAIN -c android.intent.category.HOME
```

El transporte 1 corresponde solo a esta conexión observada; verificar siempre su destino antes de reutilizarlo. Se usó `-r` para conservar datos, sin `-g`, `-d`, desinstalación o borrado de datos. Brutos: `artifacts/control-pairs-05/tcl/{package-before,home-before,install,launch,home-after}.txt` y `session.json`.

## Revisión con el mando

Probar nitidez/foco del dock; las cuatro direcciones, OK y Atrás; bajar a las apps; abrir/cerrar biblioteca; entrar en una app y volver; las cápsulas y parejas de controles; los seis fondos integrados. Home debe volver a Google TV; su pulsación física aún requiere confirmación del usuario. Mantener notificaciones apagadas para la primera muestra de rendimiento. No cambiar HDMI, audio, accesibilidad o preferencias HOME durante esta revisión. No realizar pulsaciones, ciclos automáticos ni lecturas adicionales por ADB sin aprobación.

## Salida y recuperación

Google TV permanece instalado, habilitado y resuelto como HOME. Para salir, pulsar Home. Para volver a la prueba, abrir **TCL Home · Prueba** desde la lista de apps. Ante un bloqueo, usar Ajustes → Aplicaciones → TCL Home · Prueba → Forzar detención. Si se quiere retirar, desinstalar únicamente nuestra app de prueba desde esa pantalla. Ningún paquete del sistema necesita volver a habilitarse.

B859 se conserva intacto: `artifacts/first-tcl/launcher-first-tcl-b859afc8a5b3.apk`, SHA-256 `b859afc8a5b323ee7cb16aeb921314d00bed3c04643b4a3343d2a22ed3bc37bd`. Ambos APK declaran el mismo paquete, versionCode 12 y certificado. Volver a B859 sería otra actualización de ese archivo con `install -r --user 0`, destino verificado y nueva autorización. No se ha ejecutado ni se declara ensayada esa reversión en hardware. No desinstalar o borrar datos automáticamente ante un fallo de actualización.
