package com.aprender.holaandroid.data

import java.util.Calendar
import javax.inject.Inject

/**
 * Abstracción con dos implementaciones. El módulo SaludoModule las expone
 * con @Binds + cualificador (@SaludoFormal / @SaludoInformal).
 */
interface GeneradorSaludo {
    fun saludar(nombre: String): String
}

/**
 * Inyección por constructor: Hilt sabe crear esta clase solo con @Inject.
 * El Calendar lo entrega un @Provides SIN scope, así que cada inyección
 * recibe una instancia nueva (con la hora del momento de creación).
 */
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
