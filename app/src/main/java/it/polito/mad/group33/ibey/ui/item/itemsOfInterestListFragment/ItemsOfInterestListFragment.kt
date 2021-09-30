package it.polito.mad.group33.ibey.ui.item.itemsOfInterestListFragment

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
import it.polito.mad.group33.ibey.ui.item.itemList.ItemListViewModel
import kotlinx.android.synthetic.main.item_list_fragment.*


class ItemsOfInterestListFragment : BaseItemRecycleFragment() {
    private lateinit var viewModel: ItemsOfInterestListViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.item_list_fragment, container, false)
        view.findViewById<FloatingActionButton>(R.id.idAddItem).visibility = View.GONE //TODO CHANGE CARD
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).populateToolbar(view.findViewById(R.id.toolbar))
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        userViewModel.interests.observe(viewLifecycleOwner, Observer{
            viewModel.updateAdapter(userViewModel.interests.value?: listOf())
        })
    }

    override fun getAdapter(): AdapterList {
        viewModel = ViewModelProvider(this).get(ItemsOfInterestListViewModel::class.java)
        viewModel.adapter = AdapterList(this,R.layout.item_recycle_card,R.string.emptyInterest,storageRef,R.id.action_itemsOfInterestListFragment_to_otherItemDetailsFragment,
            vClass = ItemsOfInterestListViewModel::class.java)
        return viewModel.adapter
    }

    override fun setRecyclerView(): RecyclerView {
        return itemListRecyclerView
    }

    override fun getCardClickAction():Int= R.id.action_itemsOfInterestListFragment_to_otherItemDetailsFragment
}
