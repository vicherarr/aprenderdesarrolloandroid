package com.aprender.holaandroid.domain.usecase

import com.aprender.holaandroid.data.repository.FraseRepository
import com.aprender.holaandroid.domain.frase.Frase
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import timber.log.Timber

/**
 * Convierte el posible fallo de red en un Result explícito para que el
 * ViewModel no necesite try/catch. Detalle importante: la
 * CancellationException se relanza SIEMPRE — capturarla rompería la
 * cancelación estructurada de corrutinas.
 */
class ObtenerFraseUseCase @Inject constructor(
    private val fraseRepository: FraseRepository
) {
    suspend operator fun invoke(): Result<Frase> =
        try {
            Result.success(fraseRepository.obtenerFraseAleatoria())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // WARN, no ERROR: es un fallo esperado (red) que la app maneja.
            // La excepción va como primer argumento: Timber imprime su stacktrace
            Timber.w(e, "Fallo al obtener la frase de la red")
            Result.failure(e)
        }
}
