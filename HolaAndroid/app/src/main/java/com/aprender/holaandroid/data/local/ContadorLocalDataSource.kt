package com.aprender.holaandroid.data.local

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fuente de datos local: la ÚNICA clase que sabe que el contador vive en
 * SharedPreferences. Si mañana migramos a DataStore o Room, solo cambia esto.
 */
@Singleton
class ContadorLocalDataSource @Inject constructor(
    private val prefs: SharedPreferences
) {
    fun leer(): Int = prefs.getInt(CLAVE, 0)

    fun guardar(valor: Int) {
        prefs.edit().putInt(CLAVE, valor).apply()
    }

    private companion object {
        const val CLAVE = "contador_saludos"
    }
}
