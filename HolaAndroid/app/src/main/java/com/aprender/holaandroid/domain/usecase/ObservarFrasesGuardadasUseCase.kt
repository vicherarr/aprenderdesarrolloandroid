package com.aprender.holaandroid.domain.usecase

import com.aprender.holaandroid.data.repository.FraseRepository
import com.aprender.holaandroid.domain.frase.Frase
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Expone el flujo de frases guardadas. No hay try/catch: leer de la base de
 * datos local no falla por las razones por las que falla la red, y un Flow
 * de Room emite solo cuando la tabla cambia.
 */
class ObservarFrasesGuardadasUseCase @Inject constructor(
    private val fraseRepository: FraseRepository
) {
    operator fun invoke(): Flow<List<Frase>> = fraseRepository.observarFrasesGuardadas()
}
