# Pomodoro 🍅

App Android de reloj Pomodoro con estadísticas, planta gamificada, etiquetas, sonidos ambientales y más. Hecha con Kotlin + Jetpack Compose.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## Descarga

Ve a la página de **[Releases](https://github.com/MREmmanuel09/pomodoro/releases)** y descarga el `app-release.apk` de la última versión (~3 MB).

Requisito: **Android 10 (API 29)** o superior.

## Cómo instalar el APK

Android no permite instalar apps fuera de Google Play por defecto; hay que dar permiso una vez.

### Paso 1 — Permiso de "origen desconocido"

El permiso es **por app**: se otorga a la app desde la que abres el APK (Archivos, Chrome, etc.).

1. Ajustes → **Aplicaciones** → **Acceso especial**
2. **Instalar apps desconocidas**
3. Activa el permiso para **Archivos** (y para tu navegador si instalas desde ahí)

> En Xiaomi/POCO: Ajustes → Protección de privacidad → Permisos especiales → Instalar aplicaciones de origen desconocidas.

### Paso 2 — Si Play Protect bloquea la instalación

Google Play Protect puede bloquear APKs descargados de internet aunque sean legítimos. Si ves un aviso como **"App bloqueada por Play Protect"** o la instalación no avanza:

1. Abre **Play Store** → toca tu **foto de perfil**
2. Toca **Play Protect** → **engranaje ⚙️**
3. Desactiva **"Escanear apps con Play Protect"**
4. Instala el APK desde **Archivos**
5. Al terminar, **vuelve a activar** el escaneo

También puedes buscar en el aviso la opción **"Instalar de todos modos"** (a veces está oculta tras "Detalles").

### Paso 3 — Verifica la descarga

El archivo debe pesar **~3.11 MB**. Si pesa mucho menos (o se ve como "página web"), la descarga se corrompió: bórralo y vuelve a descargarlo desde la página de Releases, tocando directamente **app-release.apk**.

### En Xiaomi/POCO (HyperOS) no instala

Si después de lo anterior sigue fallando:

1. Ajustes → Sobre el teléfono → toca **"Versión de HyperOS" 7 veces** (activa opciones de desarrollador)
2. Ajustes adicionales → Opciones para desarrolladores → baja al final
3. Toca **"Restablecer valores predeterminados" 4 veces seguidas** (desactiva la optimización del sistema)
4. Reinicia el teléfono e intenta de nuevo

## Funciones

- **Temporizador** con perfiles personalizables (clásico 25/5, 50/10, 90/20) y fases auto-avance
- **Estadísticas**: gráfico de 30 días, racha actual/récord, distribución por etiqueta
- **Planta** que crece con tus pomodoros y se marchita si la descuidas (opcional)
- **Etiquetas** por proyecto con colores (opcional)
- **Sonidos ambientales**: lluvia, olas, bosque — sintetizados, sin archivos (opcional)
- **Modo estricto**: oculta el botón de saltear fase (opcional)
- **Notificaciones y vibración** al terminar cada fase o alcanzar la meta diaria
- **Apariencia**: tema claro/oscuro/sistema, colores dinámicos (Android 12+), 5 paletas, 4 estilos de reloj
- **Exportar/importar** todos los datos en JSON
- **Español e inglés**

## Compilar desde el código

```bash
./gradlew assembleDebug    # APK de depuración
./gradlew assembleRelease  # APK de release (requiere keystore.properties)
./gradlew testDebugUnitTest
```

Abre el proyecto en Android Studio (Ladybug o superior) y usa Run ▶.

## Licencia

[MIT](LICENSE) © MREmmanuel09
