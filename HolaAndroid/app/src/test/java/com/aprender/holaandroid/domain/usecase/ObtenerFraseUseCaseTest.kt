package com.aprender.holaandroid.domain.usecase

import com.aprender.holaandroid.domain.frase.Frase
import com.aprender.holaandroid.testutil.FakeFraseRepository
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El contrato del caso de uso (guía 06): la red convertida en un Result.
 * runTest ejecuta código suspend en un dispatcher de test: los delays se
 * saltan (tiempo virtual) y las corrutinas huérfanas hacen fallar el test.
 *
 * El FakeFraseRepository nació privado en este fichero (guía 10) y se
 * promocionó a testutil/ cuando el test del ViewModel lo necesitó (guía 11).
 */
class ObtenerFraseUseCaseTest {

    private fun useCaseQueRecibe(alPedirFrase: suspend () -> Frase): ObtenerFraseUseCase =
        ObtenerFraseUseCase(FakeFraseRepository().apply { this.alPedirFrase = alPedirFrase })

    @Test
    fun `si el repositorio responde, exito con la frase`() = runTest {
        val frase = Frase(id = 7, texto = "texto", autor = "autora")
        val useCase = useCaseQueRecibe { frase }

        val resultado = useCase()

        assertEquals(Result.success(frase), resultado)
    }

    @Test
    fun `si el repositorio lanza IOException, fallo con esa excepcion`() = runTest {
        val useCase = useCaseQueRecibe { throw IOException("sin red") }

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
        val useCase = useCaseQueRecibe { throw CancellationException("cancelada") }

        useCase()
    }
}
