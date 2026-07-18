# Guía 31 — Hardware: Autenticación Biométrica y Android KeyStore (`BiometricPrompt`)

Objetivo: integrar los lectores biométricos por hardware del dispositivo (sensores dactilares bajo pantalla o en botón lateral, y reconocimiento facial 3D) mediante `BiometricPrompt` respaldado por el chip de seguridad seguro (*TEE* / StrongBox).

---

## 1. El subsistema biométrico y de seguridad (*StrongBox*)

Android gestiona la autenticación biométrica a través del procesador criptográfico seguro del dispositivo (*Trusted Execution Environment* / *StrongBox*).

Las credenciales dactilares y faciales **NUNCA abandonan el hardware seguro**; la app sólo recibe un token cifrado firmado por el hardware que confirma la validez de la identidad.

---

## 2. Permisos y Dependencias (`build.gradle.kts` y `AndroidManifest.xml`)

```xml
<uses-permission android:name="android.permission.USE_BIOMETRIC" />
```

```kotlin
dependencies {
    implementation("androidx.biometric:biometric:1.1.0")
}
```

---

## 3. Comprobar la disponibilidad del hardware biométrico

```kotlin
fun verificarHardwareBiometrico(context: Context): Boolean {
    val biometricManager = BiometricManager.from(context)
    return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
        BiometricManager.BIOMETRIC_SUCCESS -> true // Sensor dactilar/facial fuerte disponible
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> false // Dispositivo sin hardware biométrico
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> false // Sensor temporalmente deshabilitado
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> false // El usuario no ha registrado huellas
        else -> false
    }
}
```

---

## 4. Invocar el diálogo de autenticación biométrica (`BiometricPrompt`)

```kotlin
fun mostrarPromptBiometrico(
    activity: FragmentActivity,
    onExito: () -> Unit,
    onError: (String) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)

    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            onExito() // Huella / Rostro verificado correctamente por el hardware
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            onError(errString.toString())
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            // Huella no reconocida (reintento automático del sistema)
        }
    }

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Autenticación Requerida")
        .setSubtitle("Confirme su identidad con la huella dactilar para continuar")
        .setNegativeButtonText("Cancelar")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        .build()

    val biometricPrompt = BiometricPrompt(activity, executor, callback)
    biometricPrompt.authenticate(promptInfo)
}
```

---

## Fuentes consultadas (18-07-2026)

- Guía oficial de autenticación biométrica: <https://developer.android.com/identity/sign-in/biometric-auth>
- API `BiometricPrompt`: <https://developer.android.com/reference/androidx/biometric/BiometricPrompt>
