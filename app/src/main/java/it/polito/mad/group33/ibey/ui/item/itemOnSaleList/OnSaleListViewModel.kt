package it.polito.mad.group33.ibey.ui.item.itemOnSaleList

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.Query
import it.polito.mad.group33.ibey.R
import it.polito.mad.group33.ibey.repository.firebaseRepository

class OnSaleListViewModel : ViewModel() {
    var filterTitle : String = ""
    var temporaryFilterTitle : String = ""
    var filterCategoryText : String = ""
    var filterPriceStart : Double = 0.0
    var filterPriceEnd : Double = 1000.0
    var filterCategoryInt : Pair<Int, Int> = -1 to -1
    var searchBarOpen = false
    var filterState = R.id.set1_base

    fun loadItemsQueryFilteredFromFirebase(title : String, priceStart: Double, priceEnd: Double, categoryInt : Pair<Int, Int>): Query {
        return firebaseRepository.loadItemsByTitleAndPriceAndCategoryInt(title, priceStart, priceEnd, categoryInt)
    }
}