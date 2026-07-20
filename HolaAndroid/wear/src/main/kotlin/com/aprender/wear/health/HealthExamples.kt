package com.aprender.wear.health

import android.content.Context
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DeltaDataType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.guava.await

// Guía 57 §2/§3: medición puntual de frecuencia cardíaca como Flow
fun heartRateFlow(context: Context): Flow<Double> = callbackFlow {
    val measureClient = HealthServices.getClient(context).measureClient

    val callback = object : MeasureCallback {
        override fun onAvailabilityChanged(
            dataType: DeltaDataType<*, *>,
            availability: Availability
        ) {
            // reaccionar a disponibilidad del sensor
        }

        override fun onRegistered() {}

        override fun onRegistrationFailed(throwable: Throwable) {
            close(throwable)
        }

        override fun onDataReceived(data: DataPointContainer) {
            data.getData(DataType.HEART_RATE_BPM).lastOrNull()?.let { trySend(it.value) }
        }
    }

    measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, callback)
    awaitClose {
        measureClient.unregisterMeasureCallbackAsync(DataType.HEART_RATE_BPM, callback)
    }
}

// Guía 57 §2: comprobar capacidades antes de suscribirse
suspend fun soportaFrecuenciaCardiaca(context: Context): Boolean {
    val caps = HealthServices.getClient(context).measureClient.getCapabilitiesAsync().await()
    return DataType.HEART_RATE_BPM in caps.supportedDataTypesMeasure
}
