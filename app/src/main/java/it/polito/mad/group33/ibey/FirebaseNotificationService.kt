package it.polito.mad.group33.ibey

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class FirebaseNotificationService : FirebaseMessagingService() {
    override fun onMessageReceived(p0: RemoteMessage) {
        if(p0.notification != null){
            val handler = Handler(Looper.getMainLooper())
            handler.post(Runnable { Toast.makeText(applicationContext, p0.notification!!.title + ": " + p0.notification!!.body, Toast.LENGTH_SHORT).show() })
        }
        super.onMessageReceived(p0)
    }
}