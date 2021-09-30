package it.polito.mad.group33.ibey.ui.item.itemDetail

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.beloo.widget.chipslayoutmanager.util.log.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import it.polito.mad.group33.ibey.repository.firebaseRepository
import java.io.Serializable
import java.util.*

class BoughtViewModel: ViewModel() {
    inner class Transaction(val sellerUid:String,val timestamp:Timestamp,val itemId:String,val buyerUid:String,var reviewId:String? = null):Serializable
    inner class Review(val rate:Double,val description:String =""):Serializable

    var actualRate = MutableLiveData<Float>(0.0F)
    var comment = MutableLiveData<String>("")
    var dialogOpen:Boolean = false
    var reviewDone = MutableLiveData<Boolean?>(null)
    var transactionId:String = ""

    lateinit var transaction:Transaction
    lateinit var review:Review

    fun loadTransaction() {
        firebaseRepository.loadTransaction(transactionId).addOnSuccessListener {
            transaction = Transaction(it.getString("sellerUid")?:"",it.getTimestamp("timestamp")?:Timestamp(Date(0)),
                it.getString("itemId")?:"",it.getString("buyerUid")?:"",it.getString("reviewId")?:"")
            if(transaction.reviewId == null || transaction.reviewId==""){
                reviewDone.value = false
            }
            else{
                loadReview()
            }
        }.addOnFailureListener {
            Log.e("loadTransaction Log",it.message)
        }
    }

    private fun loadReview() {
        firebaseRepository.loadReview(transaction.reviewId!!).addOnSuccessListener {
            review = Review(it.getDouble("rate")?:0.0,it.getString("description")?:"")
            actualRate.value = review.rate.toFloat()
            comment.value = review.description
            reviewDone.value = true
        }.addOnFailureListener {
            Log.e("loadReview Log",it.message)
        }
    }
}