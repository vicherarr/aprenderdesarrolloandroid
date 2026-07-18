# Guía 50 — Publicación y Distribución en Google Play Store (`.aab` y Play Console)

Objetivo: dominar el proceso completo de empaquetado de producción, generación de claves criptográficas de firma (`keystore.jks`), compilación de Android App Bundles (`.aab`) y distribución en Google Play Console.

---

## 1. Generación de la Clave de Firma de Producción (`keystore.jks`)

Para publicar en Google Play, la aplicación debe estar firmada digitalmente con una clave privada.

```bash
keytool -genkeypair -v \
  -keystore mi-clave-produccion.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias mi-alias-firma
```

---

## 2. Configurar la Firma en `app/build.gradle.kts`

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("mi-clave-produccion.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "clave123"
            keyAlias = System.getenv("KEY_ALIAS") ?: "mi-alias-firma"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "clave123"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
```

---

## 3. Compilar el Android App Bundle (`.aab`)

Google Play exige publicar en formato **Android App Bundle (`.aab`)** en lugar de `.apk`. El formato `.aab` permite a Google Play generar dinámicamente APKs optimizados para la densidad de pantalla, arquitectura de CPU (arm64-v8a, x86_64) e idioma específicos del dispositivo de cada usuario.

```bash
cd HolaAndroid
./gradlew bundleRelease
```

El archivo resultante firmado queda en:
`app/build/outputs/bundle/release/app-release.aab`

---

## 4. Pasos para la Publicación en Google Play Console

1. **Creación de App en Play Console**: Definir título, descripción corta (80 chars), descripción larga (4000 chars) y categoría.
2. **Ficha de Tienda Principal**: Subir icono de app (512x512 PNG), imagen de portada (1024x500) y capturas de pantalla de móvil y tablet.
3. **Sección Seguridad de Datos (*Data Safety*)**: Declarar qué datos se recogen (ubicación, ID de dispositivo) y si se comparten con terceros.
4. **Política de Privacidad**: Enlace a la URL con la política de tratamiento de datos.
5. **Crear Lanzamiento en Pista de Pruebas Internas (*Internal Testing*)**: Subir el paquete `.aab` para probar con un grupo cerrado de testers antes de enviar a producción.

---

## Fuentes consultadas (18-07-2026)

- Formato Android App Bundle (.aab): <https://developer.android.com/guide/app-bundle>
- Firma de aplicaciones en Android: <https://developer.android.com/studio/publish/app-signing>
- Consola de desarrollo Google Play: <https://play.google.com/console/about/>
