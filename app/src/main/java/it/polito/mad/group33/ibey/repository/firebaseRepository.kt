package it.polito.mad.group33.ibey.repository

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Task
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.firestore.*
import com.google.firebase.functions.HttpsCallableResult
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import it.polito.mad.group33.ibey.Item
import it.polito.mad.group33.ibey.User
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.Serializable


object firebaseRepository {

    var firestoreDB = FirebaseFirestore.getInstance().apply { this.firestoreSettings = FirebaseFirestoreSettings.Builder().build() }
    val storage=FirebaseStorage.getInstance()
    private val functions = FirebaseFunctions.getInstance("europe-west3")
    var imgdone = MutableLiveData<Boolean>(false)

    fun saveUser(user: User): Task<Void> {
        val data:HashMap<String,Serializable> = hashMapOf(
            "fullName" to user.fullName,
            "nickname" to user.nickname,
            "phoneNumber" to user.phoneNumber,
            "location" to hashMapOf("first" to user.location.first.toString(), "second" to user.location.second.toString()))
        firestoreDB.collection("Users").document(user.uid).set(data, SetOptions.merge())
        return firestoreDB.collection("Users").document(user.uid).collection("private").document("0").set({"email" to user.email},SetOptions.merge())
    }

   // get user from firebase
    fun loadPublicUser(userId: String): DocumentReference {
       return firestoreDB.collection("Users").document(userId)
    }

    fun loadPrivateUser(userId: String): DocumentReference {
        return firestoreDB.collection("Users").document(userId).collection("private").document("0")
    }

    fun loadPrivateItem(itemId: String): DocumentReference {
        return firestoreDB.collection("Items").document(itemId).collection("private").document("0")
    }


    fun getImg(name:String,location:String) = storage.getReference(location).child(name).downloadUrl

    // ------------------------------------------------------------------- ITEM -----------------------------------------------------------------

    fun itemLikeDislike(itemId: String, like:Boolean) =    //(itemId: String, like:Boolean,button:ImageButton)
        functions.getHttpsCallable(if(like) "addInterest" else "removeInterest")
                .call(hashMapOf("id" to itemId))
                .continueWith { task ->
                                val res = task.result?.data as String
                                //button.isEnabled=true
                                //button.isClickable=true
                                Log.d("Like", res)
                                res }

    fun saveItem(item: Item): Task<String> {

        val data:HashMap<String,Serializable> = hashMapOf(
            "title" to item.title,
            "description" to item.description,
            "category" to hashMapOf("first" to item.categoryInt.first,"second" to item.categoryInt.second),
            "price" to item.price,
            "location" to hashMapOf("first" to item.location.first.toString(),"second" to item.location.second.toString()),
            "expiryDate" to item.expiryDate,
            "status" to item.statusInt)

        return if(item.id=="")
            functions.getHttpsCallable("insertItem")
                .call(data).continueWith { task -> task.result?.data as String }
            else
                firestoreDB.collection("Items").document(item.id).set(data,SetOptions.merge()).continueWith { item.id }

   }


    // get MY saved items from firebase
    fun loadMyItems(user:User): Query{
        return firestoreDB.collection("Items").whereEqualTo("sellerUid",user.uid).orderBy("timestamp",Query.Direction.DESCENDING)
    }


    // get ON SALE items from firebase
    fun loadItems(): Query {
        return firestoreDB.collection("Items").whereEqualTo("status",0)
    }

    fun loadItem(itemId:String): DocumentReference = firestoreDB.collection("Items").document(itemId)


    fun loadItemsByTitleAndPriceAndCategoryInt(title : String, priceStart: Double, priceEnd: Double, categoryInt : Pair<Int, Int>): Query {
        var query = firestoreDB.collection("Items").whereEqualTo("status",0)

        if(title != "")
            query = query.whereEqualTo("title", title)
        if(priceStart != 0.0)
            query = query.whereGreaterThanOrEqualTo("price", priceStart)
        if(priceEnd != 1000.0)
            query = query.whereLessThanOrEqualTo("price", priceEnd)
        if(categoryInt != -1 to -1)
            query = query.whereEqualTo("category", categoryInt)

        return query
    }

    fun uploadImage (image:Bitmap, location : String ,name:String):UploadTask{
        val mStorageRef = FirebaseStorage.getInstance().getReference(location)
        val imageRef : StorageReference = mStorageRef.child(name)
        val baos = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()
        return imageRef.putBytes(data)
    }

    fun notificationToken(notificationToken: String, language: String) =    //(itemId: String, like:Boolean,button:ImageButton)
        functions.getHttpsCallable("saveToken")
            .call(hashMapOf("token" to notificationToken,
                "language" to language))
            .continueWith { task ->
                val res = task.result?.data as String
                Log.d("SaveToken", res)
                res }

    // how to delete
//    fun deleteAddress(addressItem: AddressItem): Task<Void> {
//        var documentReference =  firestoreDB.collection("users/${user!!.email.toString()}/saved_addresses")
//            .document(addressItem.addressId)
//
//        return documentReference.delete()
//    }

    fun makeBuy(buyerUid: String, itemId:String) =
        functions.getHttpsCallable("makeBuy")
            .call(hashMapOf("itemId" to itemId, "buyerUid" to buyerUid))

    fun addReview(rate:Float, description:String, transactionId: String){
        functions.getHttpsCallable("addReview")
            .call(hashMapOf("rate" to rate.toDouble(), "description" to description,"tid" to transactionId))
    }

    fun loadTransaction(TransactionId: String) = firestoreDB.collection("Transactions").document(TransactionId).get()
    fun loadReview(reviewPath: String) = firestoreDB.document(reviewPath).get()

    fun loadUserReviews(userId: String):Query = firestoreDB.collection("Users").document(userId).collection("Reviews")

}