package com.aprender.holaandroid.di

import com.aprender.holaandroid.domain.saludo.GeneradorSaludo
import com.aprender.holaandroid.domain.saludo.GeneradorSaludoFormal
import com.aprender.holaandroid.domain.saludo.GeneradorSaludoInformal
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * @Binds: la forma más eficiente de decir "cuando pidan la interfaz,
 * entrega esta implementación". No genera código de construcción: la
 * implementación ya es creable por su @Inject constructor.
 *
 * Instalado en ViewModelComponent (no en SingletonComponent): estos
 * bindings solo son visibles desde ViewModels y su ciclo de vida no
 * excede el del ViewModel que los usa.
 */
@Module
@InstallIn(ViewModelComponent::class)
abstract class SaludoModule {

    @Binds
    @SaludoFormal
    abstract fun bindeaSaludoFormal(impl: GeneradorSaludoFormal): GeneradorSaludo

    @Binds
    @SaludoInformal
    abstract fun bindeaSaludoInformal(impl: GeneradorSaludoInformal): GeneradorSaludo
}
