package it.polito.mad.group33.ibey.ui.item.boughtItemsListFragment

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.Query
import it.polito.mad.group33.ibey.BaseItemsRecycleViewModel
import it.polito.mad.group33.ibey.Item
import it.polito.mad.group33.ibey.User
import it.polito.mad.group33.ibey.repository.firebaseRepository

class BoughtItemsListViewModel : BaseItemsRecycleViewModel() {
    var idToTransaction = HashMap<String,String>()
}





