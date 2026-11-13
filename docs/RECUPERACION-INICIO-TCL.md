# Inicio y recuperación: prueba previa, 2 de noviembre de 2026

## Decisión actual

El candidato `5a0247462e4b` está preparado para una actualización como aplicación. **Todavía no está validado como Inicio principal en la TCL.** No se han deshabilitado paquetes, cambiado HOME, activado accesibilidad ni reiniciado la TV en esta etapa.

La prueba del selector encontró una limitación real en el emulador Android TV API 34: asigna el rol HOME a `pe.tclhome.prototype`, pero el resolver y el botón Home siguen abriendo `com.google.android.tvlauncher/.MainActivity`, con prioridad 2. `cmd package set-home-activity` también devuelve Success sin resolverlo. El rol se conserva en dos reinicios, pero NO significa que nuestro launcher sea la pantalla de arranque. Se restauró el rol original. No se forzó la sustitución deshabilitando componentes.

En la TCL auditada, Google TV es `com.google.android.apps.tv.launcherx/.home.HomeActivity`, usuario 0, también prioridad 2. El selector oficial existe en Permission Controller. Eso hace imprescindible comprobar la actividad realmente visible al pulsar Home; no permite concluir de antemano que se comporte exactamente como el emulador.

## Problemas de CleanYourTV investigados

Fuentes primarias consultadas el 02/11/2026; sus comandos son evidencia, **no instrucciones autorizadas para ejecutar**:

* [Incidente #2557, G08/AU02/Android 14](https://github.com/Gathin23/cleanyourtv/issues/2557): bloqueo en el logo y pérdida de red tras deshabilitar 23 paquetes. El informe señala `com.android.sdksandbox` como causa probable, no aislada experimentalmente. No prueba que instalar un launcher produzca ese bloqueo. Nuestra ruta conserva todos esos paquetes y no ejecuta scripts de limpieza.
* [#1047, C6K Pro](https://github.com/Gathin23/cleanyourtv/issues/1047) y [seguimiento #2608](https://github.com/Gathin23/cleanyourtv/pull/2608): el helper de accesibilidad deja de estar enlazado después de reiniciar. Es un problema de redirección, distinto del rol HOME. No basamos la sustitución en ese helper ni concedemos AUTO_START o WRITE_SECURE_SETTINGS.
* [#1292, G08/AU02](https://github.com/Gathin23/cleanyourtv/issues/1292): deshabilitar Google TV rompió accesos físicos de apps; filtrar teclas afectó volumen mantenido. Una configuración de redirección conservó Google TV y volumen en una unidad, usando accesibilidad y AUTO_START. Queda fuera del mecanismo solicitado.
* [Caso C6K Brasil y otros G08](https://github.com/Gathin23/cleanyourtv/blob/main/examples/tcl-55c7k-uk/README.md): los reinicios documentados incluyen deshabilitaciones y decisiones de red/teclas específicas. No validan nuestra ruta sin deshabilitar paquetes ni una unidad peruana. La persistencia de ajustes y el encendido de la última entrada son pruebas diferentes de Home.
* [Troubleshooting upstream](https://github.com/Gathin23/cleanyourtv/blob/main/docs/TROUBLESHOOTING.md) y [#500](https://github.com/Gathin23/cleanyourtv/issues/500): en otros Google TV hay discrepancias entre titular del rol, prioridad del resolver y actividad realmente abierta. No elevar artificialmente la prioridad del manifiesto ni desactivar el asistente de configuración como atajo.

Android documenta [ROLE_HOME](https://developer.android.com/reference/android/app/role/RoleManager#ROLE_HOME) y [ACTION_HOME_SETTINGS](https://developer.android.com/reference/android/provider/Settings#ACTION_HOME_SETTINGS). La disponibilidad del selector y la aceptación del rol no sustituyen la prueba del mando y arranque del fabricante.

## Pruebas reproducibles realizadas

Herramienta: `tools/test-home-recovery-emulator.py`; solo admite `emulator-*`, verifica QEMU, API 34, usuario y SHA del APK. Guarda el componente original **antes** de cambiar nada y tiene restauración independiente en `finally`. No concede permisos ni deshabilita paquetes. Evidencias: `artifacts/recovery-10/characterized/`.

| Escenario | Resultado observado |
|---|---|
| Seleccionar TCL Home con D-pad en selector oficial | Rol asignado; Home efectivo sigue siendo Android TV Home. **Limitación** |
| Suspender/despertar con nuestra app visible, tres ciclos | Interfaz reanudada. Es pantalla dormida del emulador, no suspensión profunda TCL |
| Dos cierres Java forzados (`am crash`) | Home permite salir al original; nuestra app vuelve a renderizar al abrirla explícitamente con otro PID |
| Forzar detención de nuestra app | Home original accesible; apertura manual de la app funciona |
| Dos reinicios completos del emulador | Cambia boot_id; persiste el rol asignado, pero arranca el original. Nuestra app abre después y presenta sus tarjetas |
| Ajustes nativos con nuestra app detenida | Abiertos por acción del sistema, independientemente del proceso del launcher |
| Restaurar mediante selector oficial y D-pad | Rol/resolver/Home originales comprobados |
| Restaurar mediante ADB con nuestra app detenida | Rol/resolver/Home originales comprobados |
| Paquetes y permisos al finalizar | Ningún paquete deshabilitado adicional ni acceso concedido. Se comparan componentes canónicos: Android añadió un alias abreviado duplicado del listener de recomendaciones tras reiniciar |

Los primeros ensayos del arnés se conservaron: una aserción suponía incorrectamente que el rol implicaba el resolver; otra comparaba literalmente aliases equivalentes de un componente. El arnés final registra la limitación y normaliza componentes. No se borraron las evidencias fallidas ni se presentaron como fallos reparados del firmware.

**No comprobado:** autoarranque de nuestro launcher como Home efectivo, recuperación de un bucle de fallos de un Home efectivo, suspensión profunda, arranque de TCL desde apagado, comportamiento con HDMI-CEC, desconexión eléctrica ni varios días de uso. El emulador no permite certificar esas condiciones en TCL. No hay watchdog nuevo ni servicio que relance repetidamente una app que falla.

## Recuperación desde el mando: guía para la TCL

Debe ensayarse antes de dejar nuestro launcher como Inicio permanente. Es una ruta propuesta hasta verificarla en este firmware:

1. Pulsa el botón físico de Ajustes del mando para abrir los ajustes **nativos**. Conservamos Google TV y los receptores del fabricante; nuestro código solo responde si recibe la tecla estando visible.
2. Entra en aplicaciones predeterminadas / aplicación de Inicio y elige **Google TV**. La ubicación exacta dentro del menú TCL aún debe comprobarse. No elegir «Restablecer datos de fábrica».
3. Pulsa Home y comprueba visualmente que aparece Google TV. Abre una app y vuelve con Home.
4. Si no existe esa opción o el botón no abre los ajustes, usa la restauración ADB de abajo. No dejar el cambio como permanente mientras no se haya comprobado al menos una ruta desde el mando independiente de nuestro launcher.

Abrir Google TV explícitamente es una salida temporal, no cambia por sí solo el Inicio predeterminado. Desinstalar nuestra app es una última opción de retirada y borraría sus preferencias; no forma parte de esta prueba.

## Recuperación ADB independiente

Estos comandos están **preparados, no ejecutados en la TCL**. El puerto inalámbrico puede cambiar al reiniciar: leer el nuevo puerto en la TV o usar descubrimiento ADB ya autorizado. No adivinarlo ni asumir que 5555 está abierto. Si no hay red/ADB, esta vía no está disponible; por eso se exige verificar el mando primero.

En la terminal local, sustituir `IP:PUERTO` por el endpoint actual autorizado:

```text
adb -s IP:PUERTO shell am start -W -a android.settings.HOME_SETTINGS
```

Elegir Google TV con el mando y verificar. Alternativa ADB si el selector no puede usarse:

```text
adb -s IP:PUERTO shell cmd package set-home-activity --user 0 com.google.android.apps.tv.launcherx/.home.HomeActivity
adb -s IP:PUERTO shell am start -W -n com.google.android.apps.tv.launcherx/.home.HomeActivity
adb -s IP:PUERTO shell cmd package resolve-activity --brief --user 0 -a android.intent.action.MAIN -c android.intent.category.HOME
adb -s IP:PUERTO shell cmd role get-role-holders --user 0 android.app.role.HOME
```

El resolver debe nombrar `com.google.android.apps.tv.launcherx/.home.HomeActivity`, el rol debe ser `com.google.android.apps.tv.launcherx` y Home físico debe abrir Google TV. Un Success aislado no basta. No requiere que TCL Home esté ejecutándose. No tocar `setupwraith`, SystemUI, red, SDK sandbox ni datos de Google TV.

## Puertas de aprobación en la TV real

1. Actualizar solo nuestra app, comprobar Google TV como Inicio antes/después y probar diseño/fondos/mando. La autorización posterior del usuario para «ya actualiza la app» cubre esta actualización, no HOME ni reinicios.
2. Tras aprobar visualmente: leer estado habilitado de Google TV, rol/resolver/candidatos y confirmar la ruta nativa de recuperación con el mando. Proponer comandos exactos antes de ejecutarlos.
3. Con autorización específica: abrir selector oficial; usuario elige TCL Home. Comprobar Home físico desde app y desde ajustes, resolver, rol y actividad visible. **Si sigue abriendo Google TV, restaurar el rol anterior y detener esta prueba; no deshabilitar paquetes.**
4. Solo si la selección funciona y la restauración con mando/ADB se ensayó: autorizar suspensión breve, suspensión prolongada y después un reinicio del sistema. Comprobar primera pantalla antes de inyectar Home, tecla física, rol, fondo, mando y red/ADB. Repetir reinicio solo si el primero pasa. No simular corte de corriente.
5. Ante pantalla negra persistente, arranque distinto o pérdida de acceso: regresar a Google TV con la ruta comprobada y guardar el fallo. No añadir un relanzador automático para ocultarlo.

No se declara estabilidad diaria por pasar esta lista una vez. Se requiere uso real posterior sin bloquear recuperación ni servicios nativos.

## Ensayo del selector en la TCL G08/AU02 · 03/11/2026

El usuario seleccionó TCL Home en el selector oficial y reportó que Home físico seguía abriendo Google TV. La lectura ADB confirmó para el usuario 0 el titular pe.tclhome.prototype, mientras que el resolver HOME devolvió com.google.android.apps.tv.launcherx/.home.HomeActivity con prioridad 2. La actividad visible al muestrear era MainActivity de TCL Home, lo que no demuestra qué abre la tecla física. Se detuvo el ensayo sin escrituras ADB. Queda pendiente autorizar la reversión del rol al titular original.
