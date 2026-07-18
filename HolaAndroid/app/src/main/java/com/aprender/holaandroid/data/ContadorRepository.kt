package com.aprender.holaandroid.data

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * @Singleton: una única instancia para toda la app, viva mientras viva el
 * proceso. Todo el que inyecte ContadorRepository comparte el mismo estado.
 */
@Singleton
class ContadorRepository @Inject constructor(
    private val prefs: SharedPreferences
) {
    private val _contador = MutableStateFlow(prefs.getInt(CLAVE, 0))
    val contador: StateFlow<Int> = _contador.asStateFlow()

    fun incrementar() {
        val nuevo = _contador.value + 1
        prefs.edit().putInt(CLAVE, nuevo).apply()
        _contador.value = nuevo
    }

    private companion object {
        const val CLAVE = "contador_saludos"
    }
}
