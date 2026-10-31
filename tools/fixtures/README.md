# Aplicaciones de prueba para el emulador

Ocho APK independientes, sin permisos ni red, con recursos vectoriales originales. Permiten verificar dock lleno, biblioteca, apertura, retorno y cambios en paquetes sin instalar apps comerciales ni añadir tiles falsos al launcher. No representan streaming, HDMI ni reproducción real.

Compilar con el mismo JDK/SDK: `launcher/gradlew.bat -p tools/fixtures assembleDebug`. Instalar únicamente en AVD verificado, con `adb -s emulator-5554 install -r <apk>`. El launcher las descubre mediante PackageManager como cualquier otra app instalada. No distribuirlas con el APK del launcher ni instalarlas en la TV.
