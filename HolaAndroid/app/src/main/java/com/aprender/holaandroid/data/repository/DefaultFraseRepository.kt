package com.aprender.holaandroid.data.repository

import com.aprender.holaandroid.data.remote.FraseDto
import com.aprender.holaandroid.data.remote.FrasesApi
import com.aprender.holaandroid.domain.frase.Frase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * El repositorio consume la fuente remota y traduce DTO → modelo de dominio.
 * Nada por encima de esta capa sabe que existe Retrofit ni cómo es el JSON.
 */
@Singleton
class DefaultFraseRepository @Inject constructor(
    private val api: FrasesApi
) : FraseRepository {

    override suspend fun obtenerFraseAleatoria(): Frase =
        api.obtenerFraseAleatoria().aDominio()

    private fun FraseDto.aDominio() = Frase(texto = texto, autor = autor)
}
