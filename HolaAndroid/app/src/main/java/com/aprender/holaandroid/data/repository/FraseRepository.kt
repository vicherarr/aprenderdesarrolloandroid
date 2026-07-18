package com.aprender.holaandroid.data.repository

import com.aprender.holaandroid.domain.frase.Frase

interface FraseRepository {
    /** Lanza IOException/HttpException si la red falla: el dominio decide qué hacer. */
    suspend fun obtenerFraseAleatoria(): Frase
}
