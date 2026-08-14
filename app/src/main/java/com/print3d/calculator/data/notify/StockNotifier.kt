package com.print3d.calculator.data.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.print3d.calculator.R
import com.print3d.calculator.domain.model.StockStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts a system notification when a spool crosses into LOW or OUT stock. Fire-and-forget:
 * if the POST_NOTIFICATIONS grant is missing (Android 13+) it silently no-ops, so callers
 * never have to branch on permission state.
 */
@Singleton
class StockNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "low_stock"
    }

    /** Idempotent; safe to call on every app start. */
    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_stock_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = context.getString(R.string.notif_channel_stock_desc) }
        mgr.createNotificationChannel(channel)
    }

    /**
     * Notify that [label] just entered [status]. No-op for [StockStatus.OK] or when the runtime
     * notification permission is not granted. Uses [materialId] as the notification id so repeated
     * alerts for the same spool replace one another instead of stacking.
     */
    fun notifyStock(materialId: Long, label: String, remainingG: Double, status: StockStatus) {
        if (status == StockStatus.OK) return
        if (!hasPermission()) return
        ensureChannel()

        val (title, text) = when (status) {
            StockStatus.OUT -> context.getString(R.string.notif_out_stock_title) to
                context.getString(R.string.notif_out_stock_text, label)
            else -> context.getString(R.string.notif_low_stock_title) to
                context.getString(R.string.notif_low_stock_text, label, grams(remainingG))
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(materialId.toInt(), notification)
    }

    private fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun grams(v: Double): String =
        if (v % 1.0 == 0.0) v.toLong().toString() else String.format("%.1f", v)
}
