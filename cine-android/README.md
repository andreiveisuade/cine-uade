# cine-android

Puerta del acomodador en el celular: escanea el QR del ticket con la cámara y muestra **ADELANTE** o **NO PASA**.
Kotlin, Jetpack Compose (Material 3), CameraX y ML Kit Barcode Scanning (en el teléfono, solo QR), OkHttp y
kotlinx.serialization. Es un tercer cliente de la misma API que la web y Swing: manda el código a
`POST /api/acceso` con HTTP Basic y muestra lo que decide el backend; no tiene reglas de negocio.

## Compilar

Requiere JDK 17 o más y el SDK de Android (plataforma 36) con `ANDROID_HOME` o `local.properties`.

```bash
./gradlew test             # contra un servidor HTTP falso (MockWebServer), sin backend ni teléfono
./gradlew assembleDebug    # APK en app/build/outputs/apk/debug/app-debug.apk
```

## Instalar por adb

Con la depuración USB activada en el teléfono:

```bash
adb devices                                          # tiene que aparecer el equipo
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Uso

- Login con servidor, email y contraseña de un `ACOMODADOR` o `ADMINISTRADOR`. Por defecto
  `https://cine-uade.onrender.com/api`; para el docker local, `http://<IP:puerto>/api` de la PC en la misma red.
- Render free puede tardar hasta un minuto en despertar: la app avisa mientras espera.
- El resultado vuelve solo al escáner a los 4 s, o con **Escanear otra**. El código se puede escribir a mano.

## Seguridad

- La contraseña vive solo en memoria y viaja en cada pedido, como en Swing. No se guarda ni se respalda.
- Por `http` solo acepta la red propia (10.x, 172.16-31.x, 192.168.x, localhost); a internet, solo `https`.
- No sigue redirecciones, para que la clave no termine en otro host ni en `http`.
- Sin capturas de pantalla ni miniatura en recientes (`FLAG_SECURE`).
- Un error que no es el `{error}` del backend (HTML de un proxy) nunca se muestra crudo.

## Paquetes

| Paquete | Qué hay |
|---|---|
| raíz | `MainActivity` (login o Puerta), `Tema` (oscuro con el ámbar de la web), `EsperandoServidor` |
| `api/` | `ApiPuerta` (Basic, JSON y el `{error}`), `Servidor`, `Codigo`, `Rechazo` y los modelos |
| `login/` | `LoginViewModel` y `PantallaLogin` |
| `escaner/` | `PuertaViewModel`, `PantallaPuerta`, `Camara` y `AnalizadorQr` |
| `resultado/` | `PantallaResultado`, `Vibracion` y `Formato` |
