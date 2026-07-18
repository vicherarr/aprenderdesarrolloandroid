package com.aprender.holaandroid.domain.usecase

import com.aprender.holaandroid.data.repository.FraseRepository
import com.aprender.holaandroid.domain.frase.Frase
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El contrato del caso de uso (guía 06): la red convertida en un Result.
 * runTest ejecuta código suspend en un dispatcher de test: los delays se
 * saltan (tiempo virtual) y las corrutinas huérfanas hacen fallar el test.
 */
class ObtenerFraseUseCaseTest {

    /**
     * Fake configurable: el comportamiento se decide por test. Es un fake
     * escrito a mano contra la interfaz del repositorio — la recomendación
     * oficial de Android frente a las librerías de mocks para casos así.
     */
    private class FakeFraseRepository(
        private val alPedirFrase: () -> Frase
    ) : FraseRepository {
        override suspend fun obtenerFraseAleatoria(): Frase = alPedirFrase()
        override fun observarFrasesGuardadas(): Flow<List<Frase>> = flowOf(emptyList())
        override suspend fun alternarFavorita(id: Int) = Unit
    }

    @Test
    fun `si el repositorio responde, exito con la frase`() = runTest {
        val frase = Frase(id = 7, texto = "texto", autor = "autora")
        val useCase = ObtenerFraseUseCase(FakeFraseRepository { frase })

        val resultado = useCase()

        assertEquals(Result.success(frase), resultado)
    }

    @Test
    fun `si el repositorio lanza IOException, fallo con esa excepcion`() = runTest {
        val useCase = ObtenerFraseUseCase(FakeFraseRepository { throw IOException("sin red") })

        val resultado = useCase()

        assertTrue(resultado.isFailure)
        assertTrue(resultado.exceptionOrNull() is IOException)
    }

    /**
     * EL test importante: protege el matiz de la guía 06. Si alguien
     * "simplifica" el catch (o lo cambia por runCatching), la cancelación
     * quedaría atrapada en un Result y este test se pone rojo.
     */
    @Test(expected = CancellationException::class)
    fun `la cancelacion NUNCA se convierte en Result, se relanza`() = runTest {
        val useCase = ObtenerFraseUseCase(
            FakeFraseRepository { throw CancellationException("cancelada") }
        )

        useCase()
    }
}
