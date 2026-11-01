# TCL C6K Perú — diagnóstico inicial autorizado

Fecha: 31/10/2026 (America/Lima). Conexión desde la terminal local mediante vinculación inalámbrica ADB y conexión automática mDNS. Cada comando dirigido a la TV se aprobó por separado. Después de vincular, solo se realizaron lecturas. No se instalaron APK, cambiaron preferencias, activaron servicios ni modificaron paquetes. No se guardaron códigos de vinculación en archivos del proyecto.

## Hardware e interfaz observados

| Dato | Lectura real |
|---|---|
| Modelo comercial | TCL C6K, indicado por el propietario |
| Modelo de Android | Smart TV Pro |
| Device / producto | G08 / G08_4K_GB |
| SoC | MediaTek MT9689 |
| Board / hardware | tcl9618 / mt5896; plataforma merak |
| Android | 14, SDK 34 |
| Build | tcl9618-user 14 UTT2.250416.001 AU02 release-keys |
| Parche de seguridad declarado | 2025-12-05 |
| ABI | armeabi-v7a, armeabi; sistema sin ABI de apps de 64 bits |
| Pantalla física según wm | 3840×2160 |
| Override actual de interfaz | 1920×1080, ya presente antes del diagnóstico |
| Densidad | 320, sin cambio |
| RAM utilizable por el kernel | 2.451.284 kB (aproximadamente 2,34 GiB) |

MemAvailable es una foto del estado actual, no una medida de fluidez. La identidad de build coincide con la familia del informe G08/AU02 brasileño, pero no demuestra que configuración, selección de Home, arranque o permisos se comporten igual. El APK actual contiene DEX y recursos, sin bibliotecas nativas que exijan arm64; no se ha instalado en la TV.

## Inicio y restauración

Usuario Android activo: 0. Inicio resuelto: com.google.android.apps.tv.launcherx/.home.HomeActivity, prioridad 2. El rol android.app.role.HOME tiene como titular com.google.android.apps.tv.launcherx.

HOME_SETTINGS resuelve a com.google.android.permissioncontroller/com.android.permissioncontroller.role.ui.HomeSettingsActivity. Hay, por tanto, un manejador oficial que conviene probar primero después de aprobar el APK. No se abrió ese selector y no se eligió otro launcher. Su existencia no prueba todavía que acepte nuestro candidato o persista tras reiniciar.

El estado original está guardado en artifacts/tcl-readonly/home-baseline.json y el plan de vuelta en RESTAURACION-INICIO.txt. Este último no se ejecutó: la restauración real solo puede ensayarse tras aprobar un cambio reversible. Objetivo final: nuestra interfaz debe ser Home principal, manteniendo Google TV instalado y priorizando mantenerlo habilitado; no se considerará completo como acceso manual secundario.

## Paquetes, entradas y servicios

172 paquetes inventariados y 104 componentes de servicios activos en la lectura. Se conservan todos. Están presentes com.tcl.tv, tvinput, channelplus, gamebar, systemserver, systemui.plugin, globalkeyoverlay, ttvs, AirPlay, Miracast, teclado TCL y demás componentes nativos. La actividad de ttvs incluye Cast y otros servicios: no se trata como un paquete prescindible solo por contener seguimiento.

TvInputManager declara cuatro entradas HDMI: HW15/16/17/18 corresponden a hdmi_port 1/2/3/4, junto al sintonizador HW2. Es el inventario publicado por esta unidad, no un número codificado en el launcher. Falta validar etiquetas, disponibilidad y puertos físicos/dispositivos durante una prueba autorizada. No se cambió de entrada.

## Audio y volumen

En la lectura, STREAM_MUSIC usa los altavoces (speaker), escala 0–100 y volumen 10; no estaba silenciado. mIsSingleVolume=true, mUseFixedVolume=false, mExtVolumeController=null. Control de volumen CEC habilitado; mHdmiSystemAudioSupported=false en ese estado. No se interpreta esto como falta de soporte eARC ni como prueba de todas las salidas.

Esto permite investigar un control local con las APIs normales, pero no ofrece una API pública para sustituir el panel global de TCL. No se interceptaron teclas ni se alteraron volumen, audio digital, Dolby o políticas de salida. Ver VOLUMEN-EXPERIMENTAL.md.

## Evidencia y siguiente paso

Informes de dispositivo en artifacts/tcl-readonly, excluidos de Git: propiedades seleccionadas, resolución/densidad, paquetes, nombres de servicios, entradas, audio, usuario, roles y baseline Home. No publicar volcados completos con identificadores o datos de otras apps.

Siguiente paso de compatibilidad: aprobar APK y procedimiento de reversión, comprobar que el selector oficial admite nuestro launcher y verificar Home físico, salida de apps, arranque frío y reanudación. Sin esa autorización no se instala ni cambia el Inicio. Cualquier nuevo comando dirigido a la TV requiere aprobación individual según la instrucción del propietario.
