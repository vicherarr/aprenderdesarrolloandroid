package com.aprender.holaandroid.data.repository

import com.aprender.holaandroid.data.local.FraseDao
import com.aprender.holaandroid.data.local.FraseEntity
import com.aprender.holaandroid.data.remote.FraseDto
import com.aprender.holaandroid.data.remote.FrasesApi
import com.aprender.holaandroid.domain.frase.Frase
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * El repositorio coordina las DOS fuentes de datos: pide a la red (Retrofit)
 * y persiste en local (Room). Nada por encima de esta capa sabe que existen
 * una API, un JSON o una base de datos: solo ve modelos de dominio.
 */
@Singleton
class DefaultFraseRepository @Inject constructor(
    private val api: FrasesApi,
    private val fraseDao: FraseDao
) : FraseRepository {

    override suspend fun obtenerFraseAleatoria(): Frase {
        val dto = api.obtenerFraseAleatoria()
        fraseDao.insertar(dto.aEntidad())
        return dto.aDominio()
    }

    override fun observarFrasesGuardadas(): Flow<List<Frase>> =
        fraseDao.observarTodas().map { entidades -> entidades.map { it.aDominio() } }

    override suspend fun alternarFavorita(id: Int) = fraseDao.alternarFavorita(id)

    private fun FraseDto.aDominio() = Frase(id = id, texto = texto, autor = autor)

    private fun FraseDto.aEntidad() = FraseEntity(
        id = id,
        texto = texto,
        autor = autor,
        guardadaEn = System.currentTimeMillis()
    )

    private fun FraseEntity.aDominio() =
        Frase(id = id, texto = texto, autor = autor, esFavorita = favorita)
}
