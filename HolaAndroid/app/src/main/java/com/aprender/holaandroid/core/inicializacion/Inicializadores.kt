package com.aprender.holaandroid.core.inicializacion

import android.content.SharedPreferences
import javax.inject.Inject
import timber.log.Timber

/**
 * Código transversal (no pertenece a ninguna feature): vive en core/.
 * Multibinding @IntoSet — ver guía 04, sección 7.
 */
interface Inicializador {
    fun inicializar()
}

class RegistroInicializador @Inject constructor() : Inicializador {
    override fun inicializar() {
        // Sin TAG: el DebugTree de Timber usa el nombre de la clase
        Timber.d("RegistroInicializador ejecutado")
    }
}

class PrimerArranqueInicializador @Inject constructor(
    private val prefs: SharedPreferences
) : Inicializador {
    override fun inicializar() {
        if (!prefs.contains(CLAVE)) {
            prefs.edit().putLong(CLAVE, System.currentTimeMillis()).apply()
        }
        Timber.d("Primer arranque registrado en: %d", prefs.getLong(CLAVE, 0L))
    }

    private companion object {
        const val CLAVE = "primer_arranque_ms"
    }
}
