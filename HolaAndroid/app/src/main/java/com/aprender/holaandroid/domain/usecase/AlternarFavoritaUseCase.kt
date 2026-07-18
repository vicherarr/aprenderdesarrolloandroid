package com.aprender.holaandroid.domain.usecase

import com.aprender.holaandroid.data.repository.FraseRepository
import com.aprender.holaandroid.domain.frase.Frase
import javax.inject.Inject

/**
 * Marca/desmarca una frase guardada como favorita. Recibe el modelo de
 * dominio completo (no un Int suelto): la firma cuenta la intención.
 */
class AlternarFavoritaUseCase @Inject constructor(
    private val fraseRepository: FraseRepository
) {
    suspend operator fun invoke(frase: Frase) = fraseRepository.alternarFavorita(frase.id)
}
