# Adaptación nativa de Lumen

Referencia: https://github.com/samrenfrew/lumen-webos, commit cf116b640f57e4551e079539389202866a0f38b8. Se revisaron app.tsx, theme/tokens.ts, tokens.css, home.css, tiles.css, panels.css, ControlCenter.tsx, LowerVeil.tsx y blurCanvas.ts. Licencia MIT en docs/licenses y assets/licenses del APK. Implementación Java/Views sin runtime webOS ni dependencias nuevas.

## Geometría del dock

TILES_FOR[6] y reglas glass-bar: a 1920×1080, seis tarjetas de 240×144 px, separación de 46 px, margen interior de 40 px y radios de tarjeta/vidrio de 34/74 px. Dock de 1750×224 px desde x=85, tarjetas en y=846. Foco 1,12× y elevación de 7,5 px; nombre de 18 px. Conversión a unidades Android y escala proporcional en 720p. Otras configuraciones conservan el ajuste de altura heredado.

LumenDockDrawable usa negro con alpha .30, luz diagonal .13/.03/.03/.08, borde blanco/cian arriba y rosa tenue abajo. La caché contiene solo ese material translúcido: no incluye una copia opaca de la foto. Se ve el fondo real a través del dock. Durante movimiento se dibuja una textura, sin calcular blur en vivo ni capturar el fondo cada frame. Se conserva el crédito MIT del diseño trasladado.

## Al bajar a las aplicaciones

No hay título añadido. La cabecera y el crédito dejan espacio a las filas. La imagen pasa en 280 ms a una versión difuminada y oscurecida, siguiendo LowerVeil. La copia de 160×90 se prepara una vez en el hilo de carga con blur separable. En reposo se dibuja un solo fondo; durante la transición, dos. Salir de Inicio cancela la animación. Con animaciones desactivadas cambia inmediatamente. Biblioteca de apps mantiene el catálogo completo heredado, incluidas apps sin entrada Leanback.

## Centro de control

Ocho accesos, dos columnas y vecinos D-pad explícitos; filas de 52 dp, ancho 356 dp. Atenuación al 65 % solicitada por el usuario. Ajustes nativos, red, sonido, imagen, Bluetooth, entradas, aplicaciones e Inicio/personalización. Son accesos oficiales, sin interruptores o estados ficticios. Resolver de activities exportadas/habilitadas y permisos; fallback a ajustes oficiales cuando corresponda. El panel opcional settings_key_panel usa los mismos controles, sin activar accesibilidad.

## Recursos y límites

Foto Amanhecer no Hercules, Carlos Perez Couto, CC BY-SA 3.0, con atribución y fuente/licencia en APK. El JPEG se conserva sin editar; crop, oscurecimiento y copias de render mantienen esa licencia. Fuentes Inter OFL. No se importan fuentes SF privadas, recursos Apple ni logos privados de Lumen. Las apps aportan sus banners; los fixtures del emulador se identifican como aplicaciones de prueba.

Métricas anteriores y sus regresiones conservadas en PRUEBAS.md y artifacts; no se extrapolan al televisor. El firmware peruano y las integraciones TCL deben identificarse posteriormente. Fondos por horario y volumen experimental continúan en sus fases propias.
