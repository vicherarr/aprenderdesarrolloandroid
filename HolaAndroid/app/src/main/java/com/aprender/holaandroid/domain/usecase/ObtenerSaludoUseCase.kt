package com.aprender.holaandroid.domain.usecase

import com.aprender.holaandroid.di.SaludoFormal
import com.aprender.holaandroid.di.SaludoInformal
import com.aprender.holaandroid.domain.saludo.GeneradorSaludo
import javax.inject.Inject

/**
 * Caso de uso: una única responsabilidad, nombrado con la convención
 * oficial (verbo + qué + UseCase) y ejecutable como función gracias
 * al operador invoke.
 */
class ObtenerSaludoUseCase @Inject constructor(
    @SaludoFormal private val generadorFormal: GeneradorSaludo,
    @SaludoInformal private val generadorInformal: GeneradorSaludo
) {
    operator fun invoke(nombre: String, esFormal: Boolean): String {
        val generador = if (esFormal) generadorFormal else generadorInformal
        return generador.saludar(nombre)
    }
}
