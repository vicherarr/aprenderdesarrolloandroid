package com.aprender.wear.ongoing

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.aprender.wear.ui.MainActivity

private const val CANAL_ENTRENO = "entreno"

// Guía 59 §3: mostrar una Ongoing Activity para un entreno en curso
fun mostrarOngoing(context: Context, notifId: Int) {
    crearCanal(context)

    val pending = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val builder = NotificationCompat.Builder(context, CANAL_ENTRENO)
        .setSmallIcon(android.R.drawable.ic_media_play)
        .setContentTitle("Carrera en curso")
        .setOngoing(true)
        .setContentIntent(pending)

    val ongoing = OngoingActivity.Builder(context, notifId, builder)
        .setStaticIcon(android.R.drawable.ic_media_play)
        .setTouchIntent(pending)
        .setStatus(Status.Builder().addTemplate("En marcha").build())
        .build()
    ongoing.apply(context)

    NotificationManagerCompat.from(context).notify(notifId, builder.build())
}

private fun crearCanal(context: Context) {
    val canal = NotificationChannel(
        CANAL_ENTRENO, "Entreno", NotificationManager.IMPORTANCE_LOW
    )
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
}
