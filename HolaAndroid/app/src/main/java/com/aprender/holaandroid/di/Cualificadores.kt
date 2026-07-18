package com.aprender.holaandroid.di

import javax.inject.Qualifier

/**
 * Cualificadores: distinguen dos bindings del MISMO tipo dentro del grafo.
 * Sin ellos, Hilt no sabría qué [com.aprender.holaandroid.data.GeneradorSaludo] entregar.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SaludoFormal

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SaludoInformal
