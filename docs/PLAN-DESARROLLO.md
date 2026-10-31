# Plan por entregas comprobables

## Entrega 0–1 actual

1. Fijar referencias Git y auditar arquitecturas, permisos, riesgos y diferencias (AUDITORIA.md).
2. Preparar Windows con JDK 17, SDK/build-tools 34, Gradle wrapper verificado y Android TV AVD independiente.
3. Compilar el commit original antes de cambios de producción y conservar APK/log/hash como baseline local.
4. Implementar Inicio sobre navegación existente: composición original, dock, foco y retención; pruebas de D-pad y capturas del emulador, sin apps ficticias en el producto.
5. JVM + lint + compilación release; comparar arranque, PSS, CPU y frames con baseline en mismo AVD.
6. Entregar APK de desarrollo, capturas reales, limitaciones y solicitar aprobación visual. No seguir con otro rediseño mayor hasta respuesta.

## Después de aprobar Inicio

2. Fondos por cinco franjas: política pura con pruebas de límites y medianoche; importador transaccional; modo fijo/horario/vídeo; cancelar trabajo al perder visibilidad. Pruebas de ficheros inválidos, falta de espacio y reloj cambiado.
3. Centro de control: resolver settings públicos y nativos; panel D-pad con retorno de foco; ninguna accesibilidad requerida. Aprobar captura antes de ampliar.
4. HDMI: extracción del código existente, alias persistentes y estados reales. Probar vacío en AVD y hardware solo con permiso.
5. Rendimiento: carga asíncrona con generaciones si medición lo justifica; trim de bitmaps y caché acotada. No afirmar mejoras sin números.
6. TV: inventario de lectura, aprobación APK y reversión, después instalación como app sin sustituir Home; pruebas manuales de streaming, entradas, audio y reinicios. Sustitución de Inicio es otra aprobación.
7. Volumen: solo investigación separada y decisiones A/B/C de VOLUMEN-EXPERIMENTAL.md.

## Puertas de aceptación

Sin crash/ANR; foco visible y sin trampas; ninguna tecla de volumen interceptada; ninguna consulta de red nueva; sin actividades persistentes ocultas. Capturas 1280×720 y 1920×1080 reales, no escaladas para simular pruebas. Registrar cold/warm por varias muestras, gfxinfo tras recorrido idéntico, PSS y CPU tras reposo. Si las muestras son ruidosas, declararlo; el emulador solo sirve para regresiones locales.

No generar commits de APK, SDK o JDK. Mantener hashes y comandos de reproducción. Cada cambio funcional tiene commit separable; revertir por Git sin modificar estado del televisor. El código heredado no se considera automáticamente validado por compilar.
