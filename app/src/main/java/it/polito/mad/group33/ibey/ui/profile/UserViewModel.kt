package it.polito.mad.group33.ibey.ui.profile

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.EventListener
import com.google.firebase.ktx.Firebase
import it.polito.mad.group33.ibey.User
import it.polito.mad.group33.ibey.getLocationFromDocument
import it.polito.mad.group33.ibey.repository.firebaseRepository
import it.polito.mad.group33.ibey.viewmodel.ParcelableViewModel
import java.util.Collections.addAll

class UserViewModel : ParcelableViewModel() {
    var user = MutableLiveData<User>()
    var interests= MutableLiveData<List<String>>(listOf(""))
    var myItems = MutableLiveData<List<String>>(listOf(""))
    var bought = MutableLiveData<LinkedHashMap<String, String>>(linkedMapOf("" to ""))

    var img = MutableLiveData<Bitmap?>()
    var usrToken = MutableLiveData<String>("")
    lateinit var googleSignInClient: GoogleSignInClient

    private var tokenTimeExp:Long = 1

    fun keepTokenAlive() {
        Firebase.auth.currentUser?.getIdToken(true)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful){
                    usrToken.value = task.result?.token ?: ""
                    tokenTimeExp = task.result?.expirationTimestamp ?: 1
                    Log.d("userToken","token created at: " + task.result!!.issuedAtTimestamp + " expire at: " + task.result!!.expirationTimestamp)
                }
            }
    }

    override fun writeTo(bundle: Bundle) {}

    override fun readFrom(bundle: Bundle) {
    }

    fun saveUserToFirebase(user: User) : Task<Void> {
        return firebaseRepository.saveUser(user)
    }

    fun loadUserFromFirebase(userId: String,firstTime:Boolean=false){
        if(firstTime)
            user.value=User()
        firebaseRepository.loadPublicUser(userId).addSnapshotListener(EventListener { document, e ->
            if (e != null) {
                Log.w("error", "Listen failed.", e)
                // TODO in caso di errore user non viene modificato con i nuovi dati (sloggarlo?)
                return@EventListener
            }
            if (document != null) {
                user.value = User(document.getString("fullName") ?: "",document.getString("nickname") ?: "",email=user.value?.email!!,phoneNumber = document.getString("phoneNumber") ?: "",
                    location = getLocationFromDocument(document),totRate = document.getDouble("totRate")?:0.0,
                    rateCount = document.getLong("rateCount")?:-1).apply{this.uid=userId}

         }
            //TODO ELSE in caso di errore user non viene modificato con i nuovi dati (sloggarlo?)
        })

        firebaseRepository.loadPrivateUser(userId).addSnapshotListener(EventListener{
                document , e ->
            if (e != null) {
                Log.w("error", "Listen failed.", e)
                // TODO in caso di errore user non viene modificato con i nuovi dati (sloggarlo?)
                return@EventListener
            }
            if (document != null) {
                user.value = User(user.value?.fullName!!,user.value?.nickname!!,document.getString("email") ?: "",user.value?.phoneNumber!!,user.value?.location!!,user.value?.totRate!!, user.value?.rateCount!!)
                    .apply {this.uid = userId}
                interests.value =  mutableListOf("")
                    .apply {
                        val interests = document.get("interests") as List<String>?
                        if(interests != null)
                            addAll(interests)
                    }
                myItems.value = mutableListOf("")
                    .apply {
                        val myItems = document.get("myitems") as List<String>?
                        if(myItems != null)
                            addAll(myItems)
                    }
                bought.value = linkedMapOf("" to "")
                    .apply {
                        val bought = document.get("bought") as ArrayList<HashMap<String,String>>?  // TODO CAST SCHIFO CAMBIA ORDINE
                        if(bought != null){
                            bought.forEach { this.put(it["itemId"]?:"", it["transId"]?:"") }
                        }
                    }
            }
            //TODO ELSE in caso di errore user non viene modificato con i nuovi dati (sloggarlo?)
        })
    }

}
