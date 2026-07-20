package com.aprender.wear.datalayer

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.tasks.await

// Contratos compartidos (Guía 61 §3: en un proyecto real vivirían en :shared)
object DataLayerPaths {
    const val CONFIG_OBJETIVO = "/config/objetivo"
    const val ENTRENO_START = "/entreno/start"
    const val CAP_TELEFONO = "telefono_app"
}

// Guía 58 §2: enviar una orden con MessageClient
suspend fun enviarOrden(context: Context, nodeId: String) {
    val bytes = "start".toByteArray()
    Wearable.getMessageClient(context)
        .sendMessage(nodeId, DataLayerPaths.ENTRENO_START, bytes)
        .await()
}

// Guía 58 §3: escribir estado sincronizado con DataClient
suspend fun guardarObjetivo(context: Context, pasosMeta: Int) {
    val request = PutDataMapRequest.create(DataLayerPaths.CONFIG_OBJETIVO).apply {
        dataMap.putInt("pasos", pasosMeta)
        dataMap.putLong("ts", System.currentTimeMillis())
    }.asPutDataRequest().setUrgent()
    Wearable.getDataClient(context).putDataItem(request).await()
}

// Guía 58 §4: descubrir el nodo con la app de móvil
suspend fun nodoConAppTelefono(context: Context): String? {
    val info = Wearable.getCapabilityClient(context)
        .getCapability(DataLayerPaths.CAP_TELEFONO, CapabilityClient.FILTER_REACHABLE)
        .await()
    return info.nodes.firstOrNull { it.isNearby }?.id
}

// Guía 58 §2/§3: receptor de mensajes y cambios de datos
class MiWearableService : WearableListenerService() {

    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            DataLayerPaths.ENTRENO_START -> iniciarEntreno()
        }
    }

    override fun onDataChanged(events: DataEventBuffer) {
        events.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path == DataLayerPaths.CONFIG_OBJETIVO
            ) {
                val map = DataMapItem.fromDataItem(event.dataItem).dataMap
                aplicarObjetivo(map.getInt("pasos"))
            }
        }
    }

    private fun iniciarEntreno() {}
    private fun aplicarObjetivo(pasos: Int) {}
}
