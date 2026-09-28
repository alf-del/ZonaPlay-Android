# ZONA▶PLAY — COMPILACIÓN DEL APK DESDE EL CELULAR

## Objetivo
Este proyecto está preparado para que GitHub Actions compile el APK en la nube. No necesitas Android Studio ni Gradle instalado en el teléfono.

## Paso 1 — Crear el repositorio
1. Entra a GitHub desde Chrome.
2. Crea un repositorio nuevo, por ejemplo `ZonaPlay-Android`.
3. Déjalo como **Public** para simplificar el primer intento.
4. No agregues README, .gitignore ni licencia porque este proyecto ya trae sus archivos.

## Paso 2 — Subir los archivos
Extrae este proyecto ZIP en el teléfono.
En GitHub entra al repositorio y usa **Add file → Upload files**.
Sube TODOS los archivos y carpetas del proyecto, incluyendo la carpeta `.github/workflows/`.

La estructura debe comenzar así:

    ZonaPlay-Android/
      .github/
        workflows/
          build-apk.yml
      app/
        build.gradle.kts
        src/
      build.gradle.kts
      gradle.properties
      settings.gradle.kts

## Paso 3 — Ejecutar la compilación
1. Abre la pestaña **Actions** del repositorio.
2. Selecciona **ZonaPlay APK**.
3. Pulsa **Run workflow**.
4. Espera a que termine.

## Paso 4 — Descargar el APK
Cuando aparezca el trabajo en verde:
1. Entra en la ejecución terminada.
2. Baja hasta **Artifacts**.
3. Descarga `ZonaPlay-debug-apk`.
4. Dentro estará `app-debug.apk`.

## Importante
Este primer APK es una compilación de validación del proyecto híbrido Media3. La compilación exitosa confirma que la base Android puede convertirse en APK mediante GitHub Actions.

Después de validar la compilación, se hará la siguiente etapa: terminar la integración para que el reproductor nativo Media3 sea el único motor visible de reproducción y la interfaz Zona▶Play siga siendo la interfaz HTML.
