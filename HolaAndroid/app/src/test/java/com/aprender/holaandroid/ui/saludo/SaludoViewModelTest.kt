package com.aprender.holaandroid.ui.saludo

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.aprender.holaandroid.domain.frase.Frase
import com.aprender.holaandroid.domain.saludo.ComponedorTarjeta
import com.aprender.holaandroid.domain.saludo.ComponedorTarjetaFactory
import com.aprender.holaandroid.domain.saludo.GeneradorSaludo
import com.aprender.holaandroid.domain.usecase.AlternarFavoritaUseCase
import com.aprender.holaandroid.domain.usecase.EnviarSaludoUseCase
import com.aprender.holaandroid.domain.usecase.ObservarContadorUseCase
import com.aprender.holaandroid.domain.usecase.ObservarFrasesGuardadasUseCase
import com.aprender.holaandroid.domain.usecase.ObtenerFraseUseCase
import com.aprender.holaandroid.domain.usecase.ObtenerSaludoUseCase
import com.aprender.holaandroid.testutil.FakeContadorRepository
import com.aprender.holaandroid.testutil.FakeFraseRepository
import com.aprender.holaandroid.testutil.MainDispatcherRule
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * El ViewModel se testea SIN Android y SIN Hilt: se construye a mano con
 * casos de uso reales alimentados por fakes (se prueba la orquestación
 * completa dominio+ViewModel, no un ViewModel aislado con mocks).
 *
 * Turbine colecciona el StateFlow: sin coleccionista, stateIn(WhileSubscribed)
 * ni siquiera arranca el combine — igual que en producción sin pantalla.
 */
class SaludoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fraseRepo = FakeFraseRepository()
    private val contadorRepo = FakeContadorRepository()

    private fun creaViewModel(): SaludoViewModel {
        val formal = object : GeneradorSaludo {
            override fun saludar(nombre: String) = "formal:$nombre"
        }
        val informal = object : GeneradorSaludo {
            override fun saludar(nombre: String) = "informal:$nombre"
        }
        return SaludoViewModel(
            savedStateHandle = SavedStateHandle(mapOf("nombre" to "Ada")),
            observarContador = ObservarContadorUseCase(contadorRepo),
            observarFrasesGuardadas = ObservarFrasesGuardadasUseCase(fraseRepo),
            obtenerSaludo = ObtenerSaludoUseCase(formal, informal),
            enviarSaludoUseCase = EnviarSaludoUseCase(contadorRepo),
            obtenerFrase = ObtenerFraseUseCase(fraseRepo),
            alternarFavoritaUseCase = AlternarFavoritaUseCase(fraseRepo),
            tarjetaFactory = object : ComponedorTarjetaFactory {
                override fun crear(nombre: String) = ComponedorTarjeta(contadorRepo, nombre)
            }
        )
    }

    @Test
    fun `el estado inicial compone saludo formal, contador y tarjeta`() = runTest {
        val viewModel = creaViewModel()

        viewModel.uiState.test {
            // expectMostRecentItem: StateFlow conflaciona; nos quedamos con lo último
            val estado = expectMostRecentItem()

            assertEquals("formal:Ada", estado.saludo)
            assertEquals(0, estado.contador)
            assertEquals("Tarjeta para Ada — saludos enviados: 0", estado.tarjeta)
            assertEquals(FraseUiState.Inicial, estado.frase)
        }
    }

    @Test
    fun `alternar el tono cambia el saludo a informal`() = runTest {
        val viewModel = creaViewModel()

        viewModel.uiState.test {
            expectMostRecentItem()

            viewModel.alternarTono()

            val estado = awaitItem()
            assertEquals("informal:Ada", estado.saludo)
            assertEquals(false, estado.esFormal)
        }
    }

    @Test
    fun `enviar un saludo incrementa el contador en estado y tarjeta`() = runTest {
        val viewModel = creaViewModel()

        viewModel.uiState.test {
            expectMostRecentItem()

            viewModel.enviarSaludo()

            val estado = awaitItem()
            assertEquals(1, estado.contador)
            assertEquals("Tarjeta para Ada — saludos enviados: 1", estado.tarjeta)
        }
    }

    /**
     * El fake "con puerta": obtenerFrase queda suspendido en el await hasta
     * que el test lo libere. Sin la puerta, con un dispatcher Unconfined la
     * carga terminaría al instante y Cargando sería invisible (conflación).
     */
    @Test
    fun `cargar una frase pasa por Cargando y termina en Exito`() = runTest {
        val puerta = CompletableDeferred<Frase>()
        fraseRepo.alPedirFrase = { puerta.await() }
        val viewModel = creaViewModel()

        viewModel.uiState.test {
            expectMostRecentItem()

            viewModel.cargarFrase()
            assertEquals(FraseUiState.Cargando, awaitItem().frase)

            puerta.complete(Frase(id = 1, texto = "texto", autor = "autora"))
            assertEquals(FraseUiState.Exito("texto", "autora"), awaitItem().frase)
        }
    }

    @Test
    fun `si la carga falla, el estado es Error con mensaje para humanos`() = runTest {
        fraseRepo.alPedirFrase = { throw IOException("sin red") }
        val viewModel = creaViewModel()

        viewModel.uiState.test {
            expectMostRecentItem()

            viewModel.cargarFrase()

            val frase = expectMostRecentItem().frase
            assertTrue(frase is FraseUiState.Error)
            // El mensaje es de UI, no el de la excepción: eso también es contrato
            assertEquals("No se pudo cargar la frase. ¿Hay conexión?", (frase as FraseUiState.Error).mensaje)
        }
    }

    @Test
    fun `las frases guardadas fluyen de la capa de datos al estado sin accion del usuario`() = runTest {
        val viewModel = creaViewModel()

        viewModel.uiState.test {
            expectMostRecentItem()

            fraseRepo.guardadas.value = listOf(Frase(id = 1, texto = "texto", autor = "autora"))

            assertEquals(1, awaitItem().frasesGuardadas.size)
        }
    }
}
