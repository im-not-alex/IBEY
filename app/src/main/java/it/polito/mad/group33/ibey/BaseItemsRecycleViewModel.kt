package it.polito.mad.group33.ibey

import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.DiffUtil
import it.polito.mad.group33.ibey.adapters.AdapterList
import it.polito.mad.group33.ibey.ui.item.diffMyItemcallback

open class BaseItemsRecycleViewModel : ViewModel() {
    var myItemsObj = HashMap<String,Item>()
    var objectsList = listOf("")
    lateinit var adapter: AdapterList

    fun updateAdapter(new:List<String>){
        val diffResults= DiffUtil.calculateDiff(diffMyItemcallback(objectsList, new),true)
        objectsList = new
        diffResults.dispatchUpdatesTo(adapter)
    }
}