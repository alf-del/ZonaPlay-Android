# Zona▶Play — Android híbrido Media3

Proyecto Android preparado para compilar un APK mediante GitHub Actions.

La compilación automática usa Java 17 y Gradle 8.9. El workflow está en `.github/workflows/build-apk.yml` y genera `app-debug.apk` como artefacto descargable.

**Importante:** esta entrega está enfocada en resolver primero el problema de compilación/obtención del APK. El puente HTML ↔ Media3 está incluido como base de integración, pero todavía debe validarse en un Google TV real antes de considerar terminado el reproductor unificado.
