package it.polito.mad.group33.ibey.ui.profile.showReviews

import androidx.lifecycle.ViewModel
import com.firebase.ui.firestore.FirestoreArray
import com.firebase.ui.firestore.SnapshotParser
import com.google.firebase.firestore.DocumentSnapshot
import it.polito.mad.group33.ibey.User
import it.polito.mad.group33.ibey.repository.firebaseRepository
import java.io.Serializable

class ShowReviewsViewModel() : ViewModel() {
    lateinit var user:User
    var totStelline:Float=3.0F
    var authorList = HashMap<String, User>()
    lateinit var firestoreArray:FirestoreArray<ReviewWUsers>

    class ReviewWUsers(val rate:Double,val description:String ="",val authorUid:String): Serializable

    class ReviewParser(): SnapshotParser<ReviewWUsers> {
        override fun parseSnapshot(snapshot: DocumentSnapshot): ReviewWUsers {
            return ReviewWUsers(snapshot.getDouble("rate")?:0.0,snapshot.getString("description")?:"",snapshot.getString("authorUid")?: "")
        }
    }
}