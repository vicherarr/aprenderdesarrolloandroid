package com.aprender.holaandroid.data.repository

import com.aprender.holaandroid.domain.frase.Frase
import kotlinx.coroutines.flow.Flow

interface FraseRepository {
    /**
     * Pide una frase nueva a la red y la guarda en local.
     * Lanza IOException/HttpException si la red falla: el dominio decide qué hacer.
     */
    suspend fun obtenerFraseAleatoria(): Frase

    /** Las frases guardadas: favoritas primero, luego de más a menos reciente. */
    fun observarFrasesGuardadas(): Flow<List<Frase>>

    /** Marca o desmarca una frase guardada como favorita. */
    suspend fun alternarFavorita(id: Int)
}
