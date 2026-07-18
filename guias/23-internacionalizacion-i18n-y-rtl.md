# Guía 23 — Internacionalización (i18n), Soporte RTL e Idioma Per-App

Objetivo: adaptar la aplicación para usuarios globales mediante recursos multilenguaje, pluralización correcta, soporte de interfaces espejadas de derecha a izquierda (*RTL*) y preferencia de idioma independiente por aplicación en Android 13+.

---

## 1. Archivos de Recursos Cuestión de Idiomas (`values/strings.xml`)

Los textos de la app deben centralizarse en la carpeta de recursos de acuerdo a calificadores ISO del idioma:

- `res/values/strings.xml` (Idioma por defecto: Español / Inglés)
- `res/values-en/strings.xml` (Inglés)
- `res/values-fr/strings.xml` (Francés)
- `res/values-ar/strings.xml` (Árabe - Idioma RTL)

### Ejemplo de Plurales en XML (`plurals`):

```xml
<!-- res/values/strings.xml -->
<resources>
    <plurals name="articulos_guardados">
        <item quantity="one">Tienes %d artículo guardado.</item>
        <item quantity="other">Tienes %d artículos guardados.</item>
    </plurals>
</resources>
```

Uso en Jetpack Compose:
```kotlin
val textoPlural = pluralStringResource(R.plurals.articulos_guardados, count = total, total)
Text(textoPlural)
```

---

## 2. Soporte para Idiomas de Derecha a Izquierda (*RTL*)

Para idiomas como el Árabe o Hebreo, la interfaz debe espejarse automáticamente.

En Compose, se deben usar identificadores semánticos `Start` y `End` en lugar de `Left` y `Right`:

```kotlin
// INCORRECTO (No soporta RTL):
Modifier.padding(left = 16.dp, right = 8.dp)

// CORRECTO (Soporta LTR y RTL automáticamente):
Modifier.padding(start = 16.dp, end = 8.dp)
```

---

## 3. Idioma Preferido por App (Per-App Language Preferences en Android 13+)

En Android 13 (API 33+), los usuarios pueden cambiar el idioma de una app concreta sin cambiar el idioma global de todo el sistema operativo:

```xml
<!-- res/xml/locales_config.xml -->
<?xml version="1.0" encoding="utf-8"?>
<locale-config xmlns:android="http://schemas.android.com/apk/res/android">
   <locale android:name="es"/>
   <locale android:name="en"/>
   <locale android:name="fr"/>
   <locale android:name="ar"/>
</locale-config>
```

En `AndroidManifest.xml`:
```xml
<application
    android:localeConfig="@xml/locales_config">
```

Cambiar el idioma programáticamente desde un ajuste interno de la app:
```kotlin
fun cambiarIdiomaApp(context: Context, tagIdioma: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.getSystemService(LocaleManager::class.java)
            .applicationLocales = LocaleList.forLanguageTags(tagIdioma)
    } else {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tagIdioma))
    }
}
```

---

## Fuentes consultadas (18-07-2026)

- Soporte multilenguaje en Android: <https://developer.android.com/guide/topics/resources/multilingual-support>
- Configuración de idioma por aplicación: <https://developer.android.com/guide/topics/resources/app-languages>
