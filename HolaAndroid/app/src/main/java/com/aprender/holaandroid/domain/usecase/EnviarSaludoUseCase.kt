package com.aprender.holaandroid.domain.usecase

import com.aprender.holaandroid.data.repository.ContadorRepository
import javax.inject.Inject

/** Los casos de uso dependen de repositorios (capa de datos), nunca al revés. */
class EnviarSaludoUseCase @Inject constructor(
    private val contadorRepository: ContadorRepository
) {
    operator fun invoke() = contadorRepository.incrementar()
}
