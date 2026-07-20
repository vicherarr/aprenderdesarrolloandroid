package com.aprender.wear.complications

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService

// Guía 56 §7: exponer pasos como complication data source
class PasosComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        when (type) {
            ComplicationType.SHORT_TEXT -> shortText("8.2K")
            ComplicationType.RANGED_VALUE -> ranged(8200f, 10000f)
            else -> null
        }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val pasos = leerPasosDeHoy()
        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> shortText("${pasos / 1000}K")
            ComplicationType.RANGED_VALUE -> ranged(pasos.toFloat(), 10000f)
            else -> NoDataComplicationData()
        }
    }

    private fun shortText(texto: String) =
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(texto).build(),
            contentDescription = PlainComplicationText.Builder("Pasos de hoy").build()
        ).build()

    private fun ranged(valor: Float, max: Float) =
        RangedValueComplicationData.Builder(
            value = valor, min = 0f, max = max,
            contentDescription = PlainComplicationText.Builder("Progreso de pasos").build()
        ).build()

    private fun leerPasosDeHoy(): Int = 8231
}
