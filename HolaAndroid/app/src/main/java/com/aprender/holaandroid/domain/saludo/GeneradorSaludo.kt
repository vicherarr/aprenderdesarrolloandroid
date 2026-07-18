package com.aprender.holaandroid.domain.saludo

import java.util.Calendar
import javax.inject.Inject

/**
 * Lógica de negocio pura: no conoce Android ni la UI.
 * Dos implementaciones expuestas con @Binds + cualificador (di/SaludoModule).
 */
interface GeneradorSaludo {
    fun saludar(nombre: String): String
}

class GeneradorSaludoFormal @Inject constructor(
    private val calendario: Calendar
) : GeneradorSaludo {
    override fun saludar(nombre: String): String {
        val franja = when (calendario.get(Calendar.HOUR_OF_DAY)) {
            in 6..11 -> "Buenos días"
            in 12..20 -> "Buenas tardes"
            else -> "Buenas noches"
        }
        return "$franja, $nombre."
    }
}

class GeneradorSaludoInformal @Inject constructor() : GeneradorSaludo {
    override fun saludar(nombre: String) = "¡Qué pasa, $nombre!"
}
