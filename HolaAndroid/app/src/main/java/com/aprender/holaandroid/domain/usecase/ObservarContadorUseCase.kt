package com.aprender.holaandroid.domain.usecase

import com.aprender.holaandroid.data.repository.ContadorRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

class ObservarContadorUseCase @Inject constructor(
    private val contadorRepository: ContadorRepository
) {
    operator fun invoke(): StateFlow<Int> = contadorRepository.contador
}
