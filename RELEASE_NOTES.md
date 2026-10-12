Versión 1.0.1 — correcciones y mejoras.

## Correcciones
- **La planta ya no cierra la app**: al regar o completar tu primer pomodoro la app se cerraba por un error al guardar la planta. Arreglado.
- **Sonidos ambientales**: lluvia, olas y bosque ahora suenan correctamente en todos los dispositivos (se reescribió el motor de audio).
- **Notificación de meta diaria**: ahora siempre aparece al alcanzar la meta (antes podía no dispararse por una condición de carrera).
- **Cambio de perfil durante el timer**: el selector de perfiles se bloquea mientras el temporizador está corriendo o pausado (antes la interfaz quedaba inconsistente).
- **Reinicio de ronda**: al terminar una ronda completa, el botón de reproducir ahora reinicia una ronda nueva correctamente.
- **Duración real en estadísticas**: si usas los botones +1/−1 min, la sesión registra el tiempo real ajustado.
- **Rachas**: ahora se calculan bien aunque haya cambios de horario de verano.

## Mejoras
- **El temporizador sobrevive a la muerte del proceso**: si Android cierra la app con el timer corriendo, al volver a abrirla continúa donde iba (y registra la sesión si la fase terminó mientras estaba cerrada). También detecta reinicios del dispositivo para no restaurar datos inválidos.
- **Cambio de idioma inmediato**: al elegir Español/English en Ajustes, la app cambia al instante (antes requería reabrirla).
- **Rendimiento**: la notificación del timer se actualiza una vez por segundo en lugar de 4 veces (menos consumo de batería).
- **Inicio más rápido**: el idioma se lee de caché al arrancar (sin lectura de disco en el hilo principal).
- **Ayuda de la planta**: botón con explicación de cómo crece, se marchita y se recupera.

## Instalación
1. Descarga el APK de la sección Assets.
2. Permite instalar apps de "Orígenes desconocidos" para tu administrador de archivos.
3. Si Play Protect bloquea la instalación, desactiva temporalmente "Analizar apps con Play Protect" en Play Store → Play Protect → engranaje, instala y vuelve a activarlo.

Requisito: Android 10 (API 29) o superior.
