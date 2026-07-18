package com.aprender.holaandroid.ui.saludo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aprender.holaandroid.domain.saludo.ComponedorTarjetaFactory
import com.aprender.holaandroid.domain.usecase.EnviarSaludoUseCase
import com.aprender.holaandroid.domain.usecase.ObservarContadorUseCase
import com.aprender.holaandroid.domain.usecase.ObtenerSaludoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * El ViewModel orquesta casos de uso y expone UN único StateFlow<UiState>
 * (recomendación oficial). Los eventos de la UI entran como funciones
 * (alternarTono, enviarSaludo): flujo de datos unidireccional.
 */
@HiltViewModel
class SaludoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observarContador: ObservarContadorUseCase,
    private val obtenerSaludo: ObtenerSaludoUseCase,
    private val enviarSaludoUseCase: EnviarSaludoUseCase,
    private val tarjetaFactory: ComponedorTarjetaFactory
) : ViewModel() {

    private val nombre: String = savedStateHandle["nombre"] ?: "Android"

    private val esFormal = MutableStateFlow(true)

    val uiState: StateFlow<SaludoUiState> =
        combine(esFormal, observarContador()) { formal, contador ->
            SaludoUiState(
                saludo = obtenerSaludo(nombre, formal),
                contador = contador,
                esFormal = formal,
                tarjeta = tarjetaFactory.crear(nombre).componer()
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SaludoUiState()
        )

    fun alternarTono() {
        esFormal.update { !it }
    }

    fun enviarSaludo() = enviarSaludoUseCase()
}
