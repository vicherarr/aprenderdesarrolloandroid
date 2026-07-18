package com.aprender.holaandroid.ui.saludo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aprender.holaandroid.domain.saludo.ComponedorTarjetaFactory
import com.aprender.holaandroid.domain.usecase.EnviarSaludoUseCase
import com.aprender.holaandroid.domain.usecase.ObservarContadorUseCase
import com.aprender.holaandroid.domain.usecase.ObservarFrasesGuardadasUseCase
import com.aprender.holaandroid.domain.usecase.ObtenerFraseUseCase
import com.aprender.holaandroid.domain.usecase.ObtenerSaludoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SaludoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observarContador: ObservarContadorUseCase,
    observarFrasesGuardadas: ObservarFrasesGuardadasUseCase,
    private val obtenerSaludo: ObtenerSaludoUseCase,
    private val enviarSaludoUseCase: EnviarSaludoUseCase,
    private val obtenerFrase: ObtenerFraseUseCase,
    private val tarjetaFactory: ComponedorTarjetaFactory
) : ViewModel() {

    private val nombre: String = savedStateHandle["nombre"] ?: "Android"

    private val esFormal = MutableStateFlow(true)
    private val fraseState = MutableStateFlow<FraseUiState>(FraseUiState.Inicial)

    val uiState: StateFlow<SaludoUiState> =
        combine(
            esFormal,
            observarContador(),
            fraseState,
            observarFrasesGuardadas()   // Flow de Room: emite con cada cambio de la tabla
        ) { formal, contador, frase, guardadas ->
            SaludoUiState(
                saludo = obtenerSaludo(nombre, formal),
                contador = contador,
                esFormal = formal,
                tarjeta = tarjetaFactory.crear(nombre).componer(),
                frase = frase,
                frasesGuardadas = guardadas
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

    /**
     * La petición de red se lanza en viewModelScope: si el usuario abandona
     * la pantalla, la corrutina se cancela sola (cancelación estructurada).
     */
    fun cargarFrase() {
        viewModelScope.launch {
            fraseState.value = FraseUiState.Cargando
            obtenerFrase().fold(
                onSuccess = { frase ->
                    fraseState.value = FraseUiState.Exito(frase.texto, frase.autor)
                },
                onFailure = {
                    fraseState.value =
                        FraseUiState.Error("No se pudo cargar la frase. ¿Hay conexión?")
                }
            )
        }
    }
}
