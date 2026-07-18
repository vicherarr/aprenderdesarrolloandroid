package com.aprender.holaandroid.data

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/**
 * Inyección asistida: parte de los parámetros los pone Hilt
 * (contadorRepository) y parte el llamante en tiempo de ejecución (nombre).
 * No se inyecta esta clase directamente: se inyecta su @AssistedFactory.
 * Las clases @AssistedInject no admiten scope.
 */
class ComponedorTarjeta @AssistedInject constructor(
    private val contadorRepository: ContadorRepository,
    @Assisted private val nombre: String
) {
    fun componer(): String =
        "Tarjeta para $nombre — saludos enviados: ${contadorRepository.contador.value}"
}

@AssistedFactory
interface ComponedorTarjetaFactory {
    fun crear(nombre: String): ComponedorTarjeta
}
