package it.polito.mad.group33.ibey.ui.item.boughtItemsListFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.Query
import it.polito.mad.group33.ibey.*
import it.polito.mad.group33.ibey.adapters.AdapterList
import it.polito.mad.group33.ibey.repository.firebaseRepository
import it.polito.mad.group33.ibey.ui.item.itemsOfInterestListFragment.ItemsOfInterestListViewModel
import kotlinx.android.synthetic.main.item_list_fragment.*


class BoughtItemsListFragment : BaseItemRecycleFragment() {
    private lateinit var viewModel: BoughtItemsListViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.item_list_fragment, container, false)
        view.findViewById<FloatingActionButton>(R.id.idAddItem).visibility = View.INVISIBLE
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).populateToolbar(view.findViewById(R.id.toolbar))
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        userViewModel.bought.observe(viewLifecycleOwner, Observer{
            viewModel.idToTransaction=userViewModel.bought.value?: hashMapOf()
            viewModel.updateAdapter(userViewModel.bought.value?.keys?.toList()?: listOf())
        })
    }

    override fun getAdapter(): AdapterList {
        viewModel = ViewModelProvider(this).get(BoughtItemsListViewModel::class.java)
        viewModel.adapter = AdapterList(this,R.layout.item_recycle_card,R.string.emptybought,storageRef,R.id.action_boughtItemsListFragment_to_otherBoughtDetailsFragment,
            vClass = BoughtItemsListViewModel::class.java)
        return viewModel.adapter
    }

    override fun setRecyclerView(): RecyclerView {
        return itemListRecyclerView
    }

    override fun getCardClickAction():Int= R.id.action_boughtItemsListFragment_to_otherBoughtDetailsFragment
}
