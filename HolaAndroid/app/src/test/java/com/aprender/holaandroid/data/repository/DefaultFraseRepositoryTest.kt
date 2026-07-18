package com.aprender.holaandroid.data.repository

import com.aprender.holaandroid.data.local.FraseDao
import com.aprender.holaandroid.data.local.FraseEntity
import com.aprender.holaandroid.data.remote.FraseDto
import com.aprender.holaandroid.data.remote.FrasesApi
import com.aprender.holaandroid.domain.frase.Frase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * El repositorio también se testea en la JVM: FrasesApi y FraseDao son
 * interfaces (una la implementa Retrofit, otra Room... en producción; aquí,
 * dos fakes en memoria). Se prueba COMPORTAMIENTO: qué guarda, qué mapea.
 *
 * Nota: el repositorio llama a Timber. Sin árboles plantados es un no-op,
 * así que no explota — con android.util.Log directo, este test moriría con
 * "Method d in android.util.Log not mocked" (dividendo de la guía 09).
 */
class DefaultFraseRepositoryTest {

    private class FakeFrasesApi(private val respuesta: FraseDto) : FrasesApi {
        override suspend fun obtenerFraseAleatoria(): FraseDto = respuesta
    }

    /** Fake con estado: una "tabla" en memoria detrás de un StateFlow. */
    private class FakeFraseDao : FraseDao {
        private val tabla = MutableStateFlow<List<FraseEntity>>(emptyList())

        override suspend fun insertar(frase: FraseEntity) {
            // Reproduce el upsert de OnConflictStrategy.REPLACE
            tabla.value = tabla.value.filterNot { it.id == frase.id } + frase
        }

        override fun observarTodas(): Flow<List<FraseEntity>> = tabla

        override suspend fun alternarFavorita(id: Int) {
            tabla.value = tabla.value.map {
                if (it.id == id) it.copy(favorita = !it.favorita) else it
            }
        }
    }

    private val dto = FraseDto(id = 42, texto = "texto", autor = "autora")

    @Test
    fun `obtener una frase la devuelve como dominio y la persiste`() = runTest {
        val dao = FakeFraseDao()
        val repositorio = DefaultFraseRepository(FakeFrasesApi(dto), dao)

        val frase = repositorio.obtenerFraseAleatoria()

        assertEquals(Frase(id = 42, texto = "texto", autor = "autora"), frase)
        val guardadas = dao.observarTodas().first()
        assertEquals(42, guardadas.single().id)
        assertFalse(guardadas.single().favorita)
    }

    @Test
    fun `pedir dos veces la misma frase no la duplica`() = runTest {
        val dao = FakeFraseDao()
        val repositorio = DefaultFraseRepository(FakeFrasesApi(dto), dao)

        repositorio.obtenerFraseAleatoria()
        repositorio.obtenerFraseAleatoria()

        assertEquals(1, repositorio.observarFrasesGuardadas().first().size)
    }

    @Test
    fun `las entidades guardadas salen mapeadas a dominio con su favorita`() = runTest {
        val dao = FakeFraseDao()
        val repositorio = DefaultFraseRepository(FakeFrasesApi(dto), dao)
        repositorio.obtenerFraseAleatoria()

        repositorio.alternarFavorita(42)

        val frase = repositorio.observarFrasesGuardadas().first().single()
        assertEquals(Frase(id = 42, texto = "texto", autor = "autora", esFavorita = true), frase)
    }
}
