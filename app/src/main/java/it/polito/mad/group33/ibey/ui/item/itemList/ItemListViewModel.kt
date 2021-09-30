package it.polito.mad.group33.ibey.ui.item.itemList

import android.util.ArrayMap
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.DiffUtil
import com.google.firebase.firestore.Query
import it.polito.mad.group33.ibey.BaseItemsRecycleViewModel
import it.polito.mad.group33.ibey.Item
import it.polito.mad.group33.ibey.User
import it.polito.mad.group33.ibey.adapters.AdapterList
import it.polito.mad.group33.ibey.repository.firebaseRepository
import it.polito.mad.group33.ibey.ui.item.diffMyItemcallback

class ItemListViewModel : BaseItemsRecycleViewModel() {
    fun loadMyItemsQueryFromFirebase(user: User):Query {
        return firebaseRepository.loadMyItems(user)
    }
}





