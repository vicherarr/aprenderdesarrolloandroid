package com.aprender.holaandroid.core.inicializacion

import android.content.SharedPreferences
import android.util.Log
import javax.inject.Inject

/**
 * Código transversal (no pertenece a ninguna feature): vive en core/.
 * Multibinding @IntoSet — ver guía 04, sección 7.
 */
interface Inicializador {
    fun inicializar()
}

class RegistroInicializador @Inject constructor() : Inicializador {
    override fun inicializar() {
        Log.d(TAG, "RegistroInicializador ejecutado")
    }

    private companion object { const val TAG = "HolaAndroid" }
}

class PrimerArranqueInicializador @Inject constructor(
    private val prefs: SharedPreferences
) : Inicializador {
    override fun inicializar() {
        if (!prefs.contains(CLAVE)) {
            prefs.edit().putLong(CLAVE, System.currentTimeMillis()).apply()
        }
        Log.d(TAG, "Primer arranque registrado en: ${prefs.getLong(CLAVE, 0L)}")
    }

    private companion object {
        const val TAG = "HolaAndroid"
        const val CLAVE = "primer_arranque_ms"
    }
}
