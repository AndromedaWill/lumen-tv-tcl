# TCL Home será el Inicio principal

## Objetivo vinculante

La entrega final debe ser la interfaz habitual al pulsar Home, salir normalmente de una aplicación y encender la TCL C6K. Abrir un APK manualmente desde Google TV es solo el procedimiento temporal de desarrollo; no cumple el objetivo final. El launcher original debe permanecer instalado y se prioriza mantenerlo habilitado. No se declara terminado hasta validar estos comportamientos en el firmware real, con una restauración preparada y comprobada.

MainActivity ya declara MAIN + HOME + DEFAULT, es exportada y singleTask. HomeSettingsActivity ya abre Settings.ACTION_HOME_SETTINGS sin permisos especiales. Se reutilizan esos componentes, sin duplicar un segundo launcher.

## Investigación de CleanYourTV

Fuente congelada fe9ad3204b945d3aadfb6876ee6ea78c20a1fe73; archivos leídos sin ejecutar: launcher/build-and-install.ps1 y .sh; docs/TROUBLESHOOTING.md; docs/devices/nettv-sei700ntv.md; HomeKeyService.java; examples/tcl-55c7k-uk/README.md.

| Mecanismo | Evidencia y decisión |
|---|---|
| Selector oficial de Home | ACTION_HOME_SETTINGS ya existe en el fork. Primera opción si el firmware expone la pantalla y respeta la elección. No deshabilita paquetes. |
| Preferencia por ADB | CleanYourTV usa cmd package set-home-activity --user <usuario> <componente>. Success no es prueba: verificar resolver, rol, actividad real al pulsar Home y persistencia. Segunda opción mínima tras aprobación. |
| Rol HOME | ROLE_HOME/RoleManager y cmd role aparecen en el caso NetTV Android 14. Es evidencia de ese equipo, no de TCL Perú. Solo considerar si el rol existe y el firmware lo respeta. |
| Redirección con accesibilidad | stock_home_redirect + HomeKeyService puede mantener stock habilitado y redirigir cuando aparece. Puede mostrar brevemente el original y afectar repetición de volumen. No es opción por defecto; necesita análisis, un port acotado si falta en esta base y aprobación específica. |
| Deshabilitar stock | Los scripts originales pueden hacerlo automáticamente. -StockLauncher none / STOCK_LAUNCHER=none evita ese paso, pero los scripts hacen otros cambios. No se ejecutan durante desarrollo. Ninguna deshabilitación queda autorizada por este documento. |

El C6K brasileño G08/AU02 documenta launcherx deshabilitado y luego setupwraith, que había quedado resolviendo Home; no demuestra una sustitución sin deshabilitar paquetes en nuestro firmware peruano. El autor no usó helper para conservar volumen mantenido. Otros TCL G08 documentan set-home-activity con Success sin cambio efectivo mientras launcherx está habilitado. No se extrapola esa conducta a nuestra unidad.

## Qué falta identificar en la C6K peruana

La auditoría autorizada de solo lectura ya identifica la unidad: G08/AU02, Android 14, usuario 0; Home y rol HOME pertenecen a Google TV launcherx. HOME_SETTINGS tiene un manejador oficial en Permission Controller. No se abrió ni modificó. La política de encendido y la aceptación/persistencia del nuevo candidato siguen sin probar; el selector oficial es la primera ruta que se debe ensayar cuando se apruebe el APK. Ver DIAGNOSTICO-TCL-PERU.md. El plan preferido es selector oficial o preferencia/rol Home manteniendo todos los paquetes habilitados. Si el firmware los impide, se informará de esa limitación; el acceso manual no se presentará como resultado final.

Auditoría preparada: tools/audit-home.ps1 exige serial explícito y solo lee fabricante/modelo/device, Android/build, usuario, resolver/candidatos HOME, rol HOME, estado de accesibilidad y paquetes del launcher. Guarda JSON y un plan de restauración local en artifacts; no conecta por su cuenta, no selecciona dispositivo automáticamente, no instala ni cambia Home. Se ejecutó únicamente contra emulator-5554: Google TV continúa resolviendo Home. El plan de restauración generado no se ejecutó.

## Activación futura, solo con aprobación

1. Registrar componente y rol Home originales, usuario y estado habilitado de sus paquetes; guardar el informe fuera del televisor.
2. Aprobar APK, ruta de acceso a ajustes nativos y plan de vuelta al Home capturado.
3. Probar el mecanismo mínimo elegido conservando stock instalado/habilitado. No ejecutar los scripts de instalación generales.
4. Comprobar resolver/rol y actividad en primer plano, Home físico, salida de apps, arranque frío y reanudación desde standby. La política TCL de restaurar la última app/entrada puede ser distinta de Home predeterminado; usar una opción oficial de encendido si existe y se aprueba. No anular HDMI-CEC.
5. Restaurar el Home capturado mediante el selector o preferencia original y comprobar de nuevo Home, arranque y reanudación. Solo entonces documentar el procedimiento final para una persona no programadora.

Restauración mínima prevista: elegir el launcher original en Ajustes > Apps predeterminadas > Inicio, o restablecer el componente exacto registrado y verificar el resolver. Si se modificó el rol, restituir su titular registrado. Como no se deshabilitan paquetes, no hay una lista de servicios que rehabilitar. Si una verificación falla, no continuar con deshabilitaciones o accesibilidad automáticas.

## Estado actual

Compatible como candidato a Home por manifest; prototipo funcional en emulador. Inicio del televisor real y del emulador no se han cambiado. La auditoría física se realizó mediante comandos aprobados individualmente, sin ejecutar el script de auditoría en bloque. La identificación inicial de TCL Perú está completada; la sustitución efectiva y su restauración quedan pendientes de autorización y ensayo; no se promete que arranque/standby funcionen antes de verificarlos.
