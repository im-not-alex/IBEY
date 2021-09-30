package it.polito.mad.group33.ibey.ui.item.itemList

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.Query
import it.polito.mad.group33.ibey.*
import it.polito.mad.group33.ibey.adapters.AdapterList
import it.polito.mad.group33.ibey.repository.firebaseRepository
import it.polito.mad.group33.ibey.utils.FiltersMotionLayout
import it.polito.mad.group33.ibey.utils.bindView
import kotlinx.android.synthetic.main.item_list_fragment.*


class ItemListFragment : BaseItemRecycleFragment() {
    private lateinit var viewModel: ItemListViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.item_list_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).populateToolbar(view.findViewById(R.id.toolbar))
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        userViewModel.myItems.observe(viewLifecycleOwner, Observer{
            viewModel.updateAdapter(userViewModel.myItems.value?: listOf())
            recyclerView.smoothScrollToPosition(userViewModel.myItems.value?.size?:0)
        })

        val bundle = bundleOf(singleItemKey to Item().toString())
        idAddItem!!.setOnClickListener { findNavController().navigate(R.id.action_itemListFragment_to_itemEditFragment, bundle) }
    }

    override fun getAdapter(): AdapterList {
        viewModel = ViewModelProvider(this).get(ItemListViewModel::class.java)
        viewModel.adapter =
            AdapterList(this,R.layout.item_recycle_card,R.string.emptyList,storageRef,
                R.id.action_itemListFragment_to_itemDetailsFragment,R.id.action_itemListFragment_to_itemEditFragment,ItemListViewModel::class.java)
        return viewModel.adapter
    }

    override fun setRecyclerView(): RecyclerView {
        return itemListRecyclerView
    }

    override fun getCardClickAction():Int= R.id.action_itemListFragment_to_itemDetailsFragment

}
