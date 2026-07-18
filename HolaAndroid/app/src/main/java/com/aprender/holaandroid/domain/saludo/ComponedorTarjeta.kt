package com.aprender.holaandroid.domain.saludo

import com.aprender.holaandroid.data.repository.ContadorRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/**
 * Inyección asistida (ver guía 04): contadorRepository lo pone Hilt,
 * nombre lo pone el llamante en runtime.
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
