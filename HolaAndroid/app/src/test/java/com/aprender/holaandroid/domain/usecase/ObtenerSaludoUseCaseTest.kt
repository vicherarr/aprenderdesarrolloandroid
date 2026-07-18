package com.aprender.holaandroid.domain.usecase

import com.aprender.holaandroid.domain.saludo.GeneradorSaludo
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * El caso de uso elige generador según el tono. Para probar la ELECCIÓN no
 * hacen falta los generadores reales: dos fakes anónimos que devuelven una
 * marca distinguible bastan. Esto es un "stub": el doble más simple.
 */
class ObtenerSaludoUseCaseTest {

    private val formal = object : GeneradorSaludo {
        override fun saludar(nombre: String) = "formal:$nombre"
    }
    private val informal = object : GeneradorSaludo {
        override fun saludar(nombre: String) = "informal:$nombre"
    }

    private val useCase = ObtenerSaludoUseCase(formal, informal)

    @Test
    fun `con tono formal usa el generador formal`() {
        assertEquals("formal:Ada", useCase("Ada", esFormal = true))
    }

    @Test
    fun `con tono informal usa el generador informal`() {
        assertEquals("informal:Ada", useCase("Ada", esFormal = false))
    }
}
