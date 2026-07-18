# Guía 33 — Hardware: NFC (Near Field Communication)

Objetivo: aprender a interactuar con el controlador de radiofrecuencia de muy corto alcance NFC (*Near Field Communication*) para leer y escribir etiquetas NDEF (*NFC Data Exchange Format*) y soportar pagos o pases de acceso contactless.

---

## 1. El chip NFC en Android

El hardware NFC opera a una frecuencia de 13,56 MHz a distancias menores a 4 cm. Soporta tres modos de operación:
1. **Modo Lector/Escritor**: Lee y escribe datos estructurados NDEF en etiquetas pasivas (tarjetas de transporte, pegatinas NFC).
2. **Modo Emulación de Tarjeta (HCE)**: El teléfono actúa como una tarjeta bancaria o pase digital.
3. **Modo P2P (Peer-to-Peer)**: Transferencia rápida entre dispositivos.

---

## 2. Configuración en `AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.NFC" />
<!-- Declarar que NFC es un requisito obligatorio si la app no funciona sin él -->
<uses-feature android:name="android.hardware.nfc" android:required="false" />
```

---

## 3. Lectura de Etiquetas NFC (NDEF Payload)

```kotlin
class LecturaNfcActivity : ComponentActivity() {
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            if (rawMessages != null) {
                val messages = rawMessages.map { it as NdefMessage }
                for (message in messages) {
                    for (record in message.records) {
                        val payloadTexto = String(record.payload)
                        Log.d("NFC", "Contenido del Tag NFC: $payloadTexto")
                    }
                }
            }
        }
    }
}
```

---

## 4. Escritura de Mensajes NDEF en una Etiqueta NFC

```kotlin
fun escribirTextoEnTag(tag: Tag, texto: String): Boolean {
    val record = NdefRecord.createTextRecord("es", texto)
    val mensaje = NdefMessage(arrayOf(record))

    return try {
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            ndef.connect()
            if (ndef.isWritable && ndef.maxSize >= mensaje.toByteArray().size) {
                ndef.writeNdefMessage(mensaje)
                ndef.close()
                true
            } else {
                ndef.close()
                false
            }
        } else {
            false
        }
    } catch (e: Exception) {
        Log.e("NFC", "Fallo al escribir en la tarjeta NFC", e)
        false
    }
}
```

---

## Fuentes consultadas (18-07-2026)

- Conceptos básicos de NFC en Android: <https://developer.android.com/develop/connectivity/nfc>
- Emulación de tarjetas por Host (HCE): <https://developer.android.com/develop/connectivity/nfc/hce>
