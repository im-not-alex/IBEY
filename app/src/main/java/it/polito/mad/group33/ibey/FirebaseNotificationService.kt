package it.polito.mad.group33.ibey

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.DEFAULT_SOUND
import androidx.core.app.NotificationCompat.DEFAULT_VIBRATE
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class FirebaseNotificationService : FirebaseMessagingService() {
    override fun onMessageReceived(p0: RemoteMessage) {
        if(p0.notification != null){
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pendingIntent =
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
            val channelId = "Default"
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val builder =
                    NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_notification_logo)
                        .setLargeIcon(BitmapFactory.decodeResource(applicationContext.resources, R.mipmap.ic_launcher))
                        .setContentTitle(p0.notification!!.title)
                        .setContentText(p0.notification!!.body).setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(p0.notification!!.body))
                        .setDefaults(DEFAULT_SOUND or DEFAULT_VIBRATE)
                        .setPriority(NotificationManager.IMPORTANCE_HIGH)
                val channel = NotificationChannel(
                    channelId,
                    "Default channel",
                    NotificationManager.IMPORTANCE_HIGH
                )
                channel.setShowBadge(true)
                channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                manager.createNotificationChannel(channel)
                manager.notify(0, builder.build())
            } else {
                val builder =
                    NotificationCompat.Builder(this)
                        .setContentTitle(p0.notification!!.title)
                        .setContentText(p0.notification!!.body).setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(p0.notification!!.body))
                        .setDefaults(DEFAULT_SOUND or DEFAULT_VIBRATE)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setSmallIcon(R.drawable.ic_notification_logo)
                        .setLargeIcon(BitmapFactory.decodeResource(applicationContext.resources, R.mipmap.ic_launcher))

                manager.notify(0, builder.build())
            }
        }
        super.onMessageReceived(p0)
    }
}