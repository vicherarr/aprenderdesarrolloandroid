package com.aprender.holaandroid.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.Calendar
import javax.inject.Singleton

/**
 * @Provides se usa para tipos que NO puedes anotar con @Inject:
 * clases del framework, de librerías de terceros, o que requieren
 * lógica de construcción (builders, factorías...).
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * @ApplicationContext es un binding por defecto del SingletonComponent:
     * Hilt lo ofrece sin que nadie lo declare.
     */
    @Provides
    @Singleton
    fun proveeSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences("hola_android_prefs", Context.MODE_PRIVATE)

    /**
     * SIN scope: Hilt ejecuta esta función en CADA inyección.
     * Quien pida un Calendar recibe uno nuevo con la hora actual.
     */
    @Provides
    fun proveeCalendario(): Calendar = Calendar.getInstance()
}
