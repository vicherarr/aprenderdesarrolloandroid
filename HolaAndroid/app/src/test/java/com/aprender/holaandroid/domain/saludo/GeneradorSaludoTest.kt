package com.aprender.holaandroid.domain.saludo

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Lógica pura: ni Android, ni corrutinas, ni dobles. El caso más barato de
 * testear — y es así POR la arquitectura, no por suerte.
 *
 * El Calendar inyectado (guía 04) se cobra aquí: el test fabrica "las 8 de
 * la mañana" y el resultado es determinista. Con Calendar.getInstance()
 * dentro de la clase, este test pasaría o fallaría según la hora real.
 */
class GeneradorSaludoTest {

    private fun calendarioALas(hora: Int): Calendar =
        Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, hora) }

    @Test
    fun `por la manana saluda con buenos dias`() {
        val generador = GeneradorSaludoFormal(calendarioALas(8))

        assertEquals("Buenos días, Ada.", generador.saludar("Ada"))
    }

    @Test
    fun `por la tarde saluda con buenas tardes`() {
        val generador = GeneradorSaludoFormal(calendarioALas(15))

        assertEquals("Buenas tardes, Ada.", generador.saludar("Ada"))
    }

    @Test
    fun `de madrugada saluda con buenas noches`() {
        val generador = GeneradorSaludoFormal(calendarioALas(3))

        assertEquals("Buenas noches, Ada.", generador.saludar("Ada"))
    }

    @Test
    fun `los limites de las franjas caen donde dice la especificacion`() {
        // Los bordes de un when con rangos son donde viven los bugs
        assertEquals("Buenos días, Ada.", GeneradorSaludoFormal(calendarioALas(6)).saludar("Ada"))
        assertEquals("Buenas tardes, Ada.", GeneradorSaludoFormal(calendarioALas(12)).saludar("Ada"))
        assertEquals("Buenas noches, Ada.", GeneradorSaludoFormal(calendarioALas(21)).saludar("Ada"))
    }

    @Test
    fun `el informal no depende de la hora`() {
        assertEquals("¡Qué pasa, Ada!", GeneradorSaludoInformal().saludar("Ada"))
    }
}
