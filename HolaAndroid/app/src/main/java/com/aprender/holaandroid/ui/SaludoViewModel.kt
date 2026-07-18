package com.aprender.holaandroid.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aprender.holaandroid.data.ComponedorTarjetaFactory
import com.aprender.holaandroid.data.ContadorRepository
import com.aprender.holaandroid.data.GeneradorSaludo
import com.aprender.holaandroid.di.SaludoFormal
import com.aprender.holaandroid.di.SaludoInformal
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * @HiltViewModel: Hilt fabrica el ViewModel y le inyecta el constructor.
 * SavedStateHandle es un binding por defecto del ViewModelComponent.
 * Los dos GeneradorSaludo se distinguen por cualificador.
 */
@HiltViewModel
class SaludoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @SaludoFormal private val generadorFormal: GeneradorSaludo,
    @SaludoInformal private val generadorInformal: GeneradorSaludo,
    private val contadorRepository: ContadorRepository,
    private val tarjetaFactory: ComponedorTarjetaFactory
) : ViewModel() {

    private val nombre: String = savedStateHandle["nombre"] ?: "Android"

    private val _esFormal = MutableStateFlow(true)
    val esFormal: StateFlow<Boolean> = _esFormal.asStateFlow()

    val contador: StateFlow<Int> = contadorRepository.contador

    val saludo: StateFlow<String> = _esFormal
        .map { formal -> generadorActual(formal).saludar(nombre) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, generadorFormal.saludar(nombre))

    fun alternarTono() {
        _esFormal.value = !_esFormal.value
    }

    fun enviarSaludo() {
        contadorRepository.incrementar()
    }

    /** La factoría asistida mezcla dependencias del grafo con datos de runtime. */
    fun tarjeta(): String = tarjetaFactory.crear(nombre).componer()

    private fun generadorActual(formal: Boolean): GeneradorSaludo =
        if (formal) generadorFormal else generadorInformal
}
