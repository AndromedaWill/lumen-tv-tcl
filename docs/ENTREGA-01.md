# TCL Home — primera entrega de desarrollo

Base auditada el 30/10/2026; revisión visual del 31/10/2026. Rama `codex/tcl-home-prototype`. Prototipo desarrollado en emulador; posteriormente se autorizó una auditoría física de solo lectura, documentada en DIAGNOSTICO-TCL-PERU.md. No se instaló el APK ni se cambió Home en la TV. El criterio visual del usuario del 31/10 es vinculante: quitar el halo blanco y aplicar superficies coherentes al dock, botones, menús y biblioteca.

## Resultado

Inicio Java/Views sobre el fork original, seis posiciones, biblioteca desplegable hacia abajo, orden/ocultación heredados y navegación D-pad. Fondo fotográfico offline CC BY-SA 3.0 y respaldo vectorial original, Inter bajo OFL, transparencias con un borde direccional fino y reflejos localizados. Dock suavizado con una copia de 96×64, sin blur en vivo. Ampliación de foco 1,12× y elevación breve; sin animaciones el foco mantiene una ampliación instantánea de 1,06×. No se añade ninguna dependencia de producción.

`HomeFocusStore` conserva identidad de la app y posición al salir; retorno con Atrás y reconstrucción del proceso probados en emulador. Atrás regresa al dock en vez de invocar Somnambulator de SystemUI. La biblioteca queda fuera de pantalla en reposo, sin trozos de tiles recortados en el borde inferior. Las tiles tienen descripción accesible con su nombre real.

`SystemSettings` adapta el resolver reciente de CleanYourTV y comprueba activity exportada, habilitada y permiso de lanzamiento. Los destinos TCL son candidatos verificados en tiempo de ejecución, no una afirmación de compatibilidad con firmware peruano. La adaptación visual solicitada de Lumen añade un Centro de control nativo de ocho accesos y nombres bajo el icono enfocado. Los estados detallados y controles específicos TCL siguen pendientes; ver ADAPTACION-LUMEN.md.

El original compila y pasa 124 pruebas, pero lint detectó tres BOM literales y su APK ya supera el antiguo gate de 400 KiB del fork. Los BOM se corrigieron en un commit aislado, sin suprimir lint. No se cambió el gate heredado ni se presenta su CI como aprobada.

## Archivos y reproducción

- [Auditoría y lista de ports](AUDITORIA.md).
- [Arquitectura y contratos de siguientes componentes](ARQUITECTURA.md).
- [Compatibilidad TCL y salida segura](COMPATIBILIDAD-TCL.md).
- [Inicio principal: mecanismos, selección y restauración](INICIO-PRINCIPAL.md).
- [Diagnóstico real de TCL Perú, solo lectura](DIAGNOSTICO-TCL-PERU.md).
- [Plan con aprobación visual entre fases](PLAN-DESARROLLO.md).
- [Windows y comandos de compilación](ENTORNO-WINDOWS.md).
- [Volumen experimental, sin activar](VOLUMEN-EXPERIMENTAL.md).
- [Pruebas, métricas y pendientes](PRUEBAS.md).
- [Adaptación visual de Lumen y licencia](ADAPTACION-LUMEN.md).

Los APK, capturas PNG sin editar, jerarquías UI y logs se conservan en `artifacts/` local, excluido de Git. APK actual: `artifacts/tcl-home-prototype.apk`, paquete `pe.tclhome.prototype`, firma de desarrollo. El APK original de comparación es otro paquete y se conserva intacto.

Las capturas incluyen YouTube y otras aplicaciones del AVD, junto a ocho APK independientes de prueba con iconos originales (`tools/fixtures`). Estos últimos no son servicios comerciales ni funciones HDMI y nunca forman parte del APK del launcher. Su propósito es comprobar catálogo, apertura y retorno sin falsear la lista de PackageManager.

## Todavía no es una versión diaria aprobada

Pendiente aprobación visual de Inicio. No se han implementado todavía importación de cinco imágenes/horarios, Centro de control completo, alias HDMI ni indicador de volumen. Sus componentes y restricciones están definidos en ARQUITECTURA.md. Los aéreos heredados están desactivados por defecto; no se descargó contenido Apple ni se certificó su reproducción.

Los servicios de accesibilidad no se activan. No se seleccionó el launcher como Inicio; no se deshabilitó, desinstaló ni sustituyó ningún paquete. No se modificó resolución, densidad, refresco o escalas de animación del televisor. Las resoluciones de prueba pertenecen a dos AVD separados.

## Volver atrás

En emulador, Inicio sigue abriendo su launcher original. Para una prueba futura en TCL, primero identificar firmware y aprobar APK/procedimiento; instalar como app normal y salir con Inicio. No aplicar ningún script de debloat. La reversión del código usa commits descriptivos; SDK/JDK y artefactos locales no se mezclan con las fuentes.

## Revisión solicitada del dock y biblioteca

Material más oscuro y con menos capas de reflejo, borde fino y espacio vertical ajustado. Se conserva el centro compacto, separado del dock y con atenuación de fondo al 65 %. Al bajar, la cabecera se oculta y el fondo se difumina y las filas no llevan un título adicional. El botón Biblioteca de apps abre el catálogo completo heredado, incluyendo las apps sin banner Leanback. Las capturas distinguen la vista al bajar y ese catálogo.
