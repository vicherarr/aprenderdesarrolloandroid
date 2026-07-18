# Guía 46 — Seguridad Avanzada: Cifrado en Reposo, ProGuard/R8 y Certificate Pinning

Objetivo: proteger los datos sensibles de la aplicación mediante cifrado criptográfico con Jetpack Security, asegurar las peticiones HTTP contra ataques MITM mediante *Certificate Pinning* y proteger el APK contra ingeniería inversa con reglas R8/ProGuard.

---

## 1. Cifrado de Datos en Reposo (`EncryptedSharedPreferences`)

Guardar tokens de sesión o claves API en texto plano es una vulnerabilidad crítica. La librería `androidx.security` utiliza el chip criptográfico seguro de Android para cifrar claves y valores:

```kotlin
dependencies {
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}
```

```kotlin
fun obtenerSharedPreferencesCifradas(context: Context): SharedPreferences {
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    return EncryptedSharedPreferences.create(
        context,
        "credenciales_seguras",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}
```

---

## 2. Prevención de Ataques Man-in-the-Middle (*Certificate Pinning*)

El *Certificate Pinning* bloquea la comunicación de red a menos que la clave pública SHA-256 del certificado del servidor coincida exactamente con las huellas fijadas en la app:

```kotlin
val certificatePinner = CertificatePinner.Builder()
    .add("api.mi-aplicacion.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
    .build()

val okHttpClient = OkHttpClient.Builder()
    .certificatePinner(certificatePinner)
    .build()
```

---

## 3. Minificación y Ofuscación con R8/ProGuard

R8 elimina código muerto, renombra clases/métodos a letras indescifrables (`a.b.c`) y optimiza el APK en Builds de Release.

En `app/build.gradle.kts`:
```kotlin
android {
    buildTypes {
        release {
            isMinifyEnabled = true // Activa ofuscación y borrado de código muerto
            isShrinkResources = true // Elimina recursos (imágenes, xml) sin usar
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

En `app/proguard-rules.pro` (Evitar que R8 rompa clases serializadas o de Retrofit/Room):
```proguard
# Preservar modelos DTO usados en Kotlin Serialization
-keepattributes *Annotation*,Signature
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
```

---

## Fuentes consultadas (18-07-2026)

- Jetpack Security Crypto: <https://developer.android.com/topic/security/data>
- Ofuscación y minificación con R8: <https://developer.android.com/build/shrink-code>
