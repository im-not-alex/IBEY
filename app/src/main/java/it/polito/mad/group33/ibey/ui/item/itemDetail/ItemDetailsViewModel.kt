package it.polito.mad.group33.ibey.ui.item.itemDetail

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.DiffUtil
import com.google.firebase.firestore.EventListener
import it.polito.mad.group33.ibey.Item
import it.polito.mad.group33.ibey.User
import it.polito.mad.group33.ibey.getLocationFromDocument
import it.polito.mad.group33.ibey.getUserFromDocument
import it.polito.mad.group33.ibey.repository.firebaseRepository


class ItemDetailsViewModel : ViewModel() {
    var myItem:Item?=null
    var interest:Boolean? = null
    var img = MutableLiveData<Bitmap?>()
    var imgModified:Boolean = false
    var sellerAquired = MutableLiveData<Boolean>(false)
    var clickable=0
    var isIdSellOpen : Boolean = false
    var isIdEditOpen : Boolean = false
    var usersInterested = MutableLiveData<MutableList<String>>(mutableListOf())
    lateinit var adapter:OwnItemDetailsFragment.ChipsAdapter
    var interested = HashMap<String,User>()
    var buyerUid = MutableLiveData<String?>(null)
    var reviewPath = MutableLiveData<String>("")
    fun retrieveSeller(userid: String) {
        firebaseRepository.loadPublicUser(userid).addSnapshotListener(EventListener { document, e ->
            if (e != null) {
                Log.w("error", "Listen failed.", e)
                // TODO in caso di errore user non viene caricato
                return@EventListener
            }
            if (document != null) {
                val updatedUser = User(document.getString("fullName") ?: "",document.getString("nickname") ?: "",
                    phoneNumber = document.getString("phoneNumber") ?: "",location = getLocationFromDocument(document),
                    totRate = document.getDouble("totRate")?:0.0, rateCount = document.getLong("rateCount")?:-1)
                updatedUser.uid = userid
                myItem?.seller = updatedUser
                sellerAquired.value = true
            }
            //TODO ELSE in caso di errore user non viene modificato con i nuovi dati (sloggarlo?)
        })
    }

    fun retrieveInterests(itemId: String) {
        firebaseRepository.loadPrivateItem(itemId).addSnapshotListener(EventListener { document, e ->
            if (e != null) {
                Log.w("error", "Listen failed.", e)
                // TODO in caso di errore user non viene caricato
                return@EventListener
            }
            if (document != null) {
                buyerUid.value = document.getString("buyerUid")
                reviewPath.value = document.getString("reviewId")?:""
                val new = if(buyerUid.value!=null && buyerUid.value!="" && buyerUid.value!="other"){
                    arrayListOf(buyerUid.value!!).apply {
                        val retrievedInterest = (document.get("interests") as ArrayList<String>)
                        retrievedInterest.remove(buyerUid.value!!)
                        addAll(retrievedInterest)
                    }
                }
                else
                    document.get("interests") as ArrayList<String>? ?: arrayListOf()

                val diffResults=DiffUtil.calculateDiff(diffcallback(usersInterested.value!!.toList(),new),false)

                Log.d("deb",usersInterested.value.toString())
                var diffList = new.minus(interested.keys)
                var counter = 0
                if(diffList.isEmpty()){
                    usersInterested.value=new
                    diffResults.dispatchUpdatesTo(adapter)
                }else{
                    diffList.forEach {
                        firebaseRepository.loadPublicUser(it)
                            .addSnapshotListener(EventListener { document, e ->
                                counter++
                                if (e != null) {
                                    Log.w("error", "Listen failed.", e)
                                    // TODO in caso di errore user non viene caricato
                                    return@EventListener
                                }
                                if (document != null) {
                                    interested[it] = getUserFromDocument(document).apply { this.uid = it }
                                }
                                if(counter==diffList.size){
                                    usersInterested.value=new
                                    diffResults.dispatchUpdatesTo(adapter)
                                }
                            })
                    }
                }

            }
        })
    }

    fun clear() {
        interest=null
        imgModified=false
        clickable=0
        isIdEditOpen=false
        isIdSellOpen=false
        usersInterested.value= mutableListOf()
        buyerUid.value=null
        reviewPath.value=""
    }

    class diffcallback(val old:List<String>,val new:List<String>) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldUserPosition: Int, newUserPosition: Int): Boolean {
            return old[oldUserPosition] == new[newUserPosition]
        }

        override fun getOldListSize(): Int {
            return old.size
        }

        override fun getNewListSize(): Int {
            return new.size
        }

        override fun areContentsTheSame(oldUserPosition: Int, newUserPosition: Int): Boolean {
            return old[oldUserPosition] == new[newUserPosition]
        }

    }



}
