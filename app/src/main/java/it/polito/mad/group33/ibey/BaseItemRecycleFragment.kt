package it.polito.mad.group33.ibey

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.Observer
import it.polito.mad.group33.ibey.adapters.AdapterList
import it.polito.mad.group33.ibey.repository.firebaseRepository
import it.polito.mad.group33.ibey.ui.profile.UserViewModel

abstract class BaseItemRecycleFragment : Fragment() {
    protected lateinit var userViewModel: UserViewModel
    lateinit var recyclerView:RecyclerView
    val storageRef = firebaseRepository.storage.getReference("Users")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)

    //    val query: Query = getItemAdapterRQuery()
    //    val options: FirestoreRecyclerOptions<Item> = FirestoreRecyclerOptions.Builder<Item>().setQuery(query, ItemParser(requireContext())).build()
        recyclerView = setRecyclerView()
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this.context,RecyclerView.VERTICAL,true).apply { stackFromEnd=true }
        recyclerView.adapter = getAdapter()
    }

    protected abstract fun getAdapter(): AdapterList

    // Used in onActivityCreated. Be sure that every lateinit variable used in this method
    // it's initialized before super.onActivityCreated
    //protected abstract fun getItemAdapterRQuery(): Query

    // Used in onActivityCreated. Be sure that every lateinit variable used in this method
    // it's initialized before super.onActivityCreated
    protected abstract fun setRecyclerView() : RecyclerView

    protected abstract fun getCardClickAction():Int

}