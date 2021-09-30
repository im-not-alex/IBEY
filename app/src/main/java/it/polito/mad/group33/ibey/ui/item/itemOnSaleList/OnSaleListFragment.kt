package it.polito.mad.group33.ibey.ui.item.itemOnSaleList

import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.paging.PagedList
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.google.android.material.slider.RangeSlider
import com.google.firebase.firestore.Query
import it.polito.mad.group33.ibey.*
import it.polito.mad.group33.ibey.adapters.ItemAdapterFirestoreP
import it.polito.mad.group33.ibey.repository.firebaseRepository
import it.polito.mad.group33.ibey.ui.item.ItemParser
import it.polito.mad.group33.ibey.ui.profile.UserViewModel
import it.polito.mad.group33.ibey.utils.FiltersMotionLayout
import it.polito.mad.group33.ibey.utils.bindView
import kotlinx.android.synthetic.main.item_list_fragment.*
import kotlinx.android.synthetic.main.item_list_fragment.itemListRecyclerView
import kotlinx.android.synthetic.main.layout_filter_motion.*
import kotlinx.android.synthetic.main.other_item_list.*
import java.text.DecimalFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round


class OnSaleListFragment : Fragment(),SwipeRefreshLayout.OnRefreshListener {
    private lateinit var viewModel: OnSaleListViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var adapter: ItemAdapterFirestoreP
    private lateinit var config: PagedList.Config
    private val filtersMotionLayout: FiltersMotionLayout by bindView(R.id.filters_motion_layout)
    private var state = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.other_item_list, container, false)
        setHasOptionsMenu(true)
        retainInstance = true
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).populateToolbar(view.findViewById(R.id.toolbar))
        swipeRefresh.setOnRefreshListener(this)
        //swipeRefresh.isRefreshing = true

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.onsalelistbar, menu)

        val searchItem: MenuItem? = menu.findItem(R.id.app_bar_search)
        searchItem!!.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                viewModel.searchBarOpen = true
                val searchView: SearchView? = searchItem.actionView as SearchView
                searchView!!.isIconifiedByDefault = false
                searchView.isIconified = false
                searchView.isFocusable = true
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                requireActivity().hideKeyboard()
                viewModel.searchBarOpen = false
                return true
            }
        })

        val searchView: SearchView? = searchItem.actionView as SearchView
        searchView!!.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                hideKeyboard()
                if(query != null) {
                    viewModel.temporaryFilterTitle = ""
                    viewModel.filterTitle = query
                    manageFilterQuery()
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.temporaryFilterTitle = newText ?: ""

                if(newText == "") {
                    viewModel.filterTitle = ""
                    manageFilterQuery()
                }
                return true
            }
        })

        if(viewModel.searchBarOpen) {
            if(viewModel.temporaryFilterTitle != "")
                searchView.setQuery(viewModel.temporaryFilterTitle, false)
            else
                searchView.setQuery(viewModel.filterTitle, false)
        }

        super.onCreateOptionsMenu(menu, inflater)
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(OnSaleListViewModel::class.java)
        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)
/*        if(listOf(R.id.set1_base,R.id.set4_settle,R.id.set10_unfilterOutset).contains(viewModel.filterState))
            filtersMotionLayout.transitionToState(viewModel.filterState)*/
        config = PagedList.Config.Builder().setEnablePlaceholders(false).setPrefetchDistance(10).setPageSize(20).build() // TODO check parametri: iniziali sono false 1 10
        val options: FirestorePagingOptions<Item> = FirestorePagingOptions.Builder<Item>().setLifecycleOwner(this).setQuery(getItemsQuery(), config, ItemParser()).build()
        val storageRef = firebaseRepository.storage.getReference("Users")
        adapter = ItemAdapterFirestoreP(options, this, true, userViewModel.user.value!!, storageRef
        )
        var spanCount = 1
        if(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
            spanCount = 2

        val manager = GridLayoutManager(this.context,spanCount,GridLayoutManager.VERTICAL,false)
        itemListRecyclerView.layoutManager = manager

        itemListRecyclerView.adapter = adapter

        //size of nav backstack printed in onsalelistfragment and loginfragment
        Log.d("debug","stack size:"+(activity as MainActivity).supportFragmentManager.findFragmentById(R.id.nav_host_fragment)?.childFragmentManager?.backStackEntryCount.toString())

        categoryDropDown()

        if(viewModel.filterCategoryText != "")
            filter_popup_filled_exposed_dropdown.setText(viewModel.filterCategoryText)

        val decimalFormat = DecimalFormat.getCurrencyInstance(Locale.ITALY)
        if(viewModel.filterPriceStart != 0.0 || viewModel.filterPriceEnd != 1000.0)
            filterPopupPriceDisplay.text = "From" + decimalFormat.format(viewModel.filterPriceStart) + " to " + decimalFormat.format(viewModel.filterPriceEnd)
        else
            filterPopupPriceDisplay.text = getString(R.string.allPrices)

        filter_popup_id_item_price.addOnChangeListener { slider, _, _ ->
            viewModel.filterPriceStart = min(slider.values[0], slider.values[1]).toDouble()
            viewModel.filterPriceEnd = max(slider.values[0], slider.values[1]).toDouble()
            when {
                viewModel.filterPriceEnd != 1000.0 -> filterPopupPriceDisplay.text =
                    "From " + decimalFormat.format(viewModel.filterPriceStart) + " to " + decimalFormat.format(viewModel.filterPriceEnd)
                viewModel.filterPriceStart != 0.0 -> filterPopupPriceDisplay.text = "From " +  decimalFormat.format(viewModel.filterPriceStart)
                else -> filterPopupPriceDisplay.text = getString(R.string.allPrices)

            }
        }
    }

    override fun onStart() {
        super.onStart()
        adapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
    }

    fun clearFilterAndExecuteQuery()
    {
        viewModel.filterCategoryText=""
        viewModel.filterPriceStart = 0.0
        viewModel.filterPriceEnd = 1000.0
        viewModel.filterCategoryInt = -1 to -1
        filter_popup_filled_exposed_dropdown.setText("")
        filterPopupPriceDisplay.text = "All prices"
        filter_popup_id_item_price.values = listOf(0f, 1000f)
        manageFilterQuery()
    }

    fun manageFilterQueryWithPriceExtraction()
    {
        viewModel.filterPriceStart = round(min(filter_popup_id_item_price.values[0], filter_popup_id_item_price.values[1]).toDouble() * 100.0) / 100.0
        viewModel.filterPriceEnd = round(max(filter_popup_id_item_price.values[0], filter_popup_id_item_price.values[1]).toDouble() * 100.0) / 100.0
        manageFilterQuery()
    }

    fun manageFilterQuery()
    {
        (itemListRecyclerView.adapter as ItemAdapterFirestoreP)
            .updateOptions(
                FirestorePagingOptions.Builder<Item>().setLifecycleOwner(
                    this
                ).setQuery(getItemsQuery(), config, ItemParser()).build()
            )
    }

    private fun getItemsQuery() : Query = viewModel.loadItemsQueryFilteredFromFirebase(
            viewModel.filterTitle,
            viewModel.filterPriceStart,
            viewModel.filterPriceEnd,
            viewModel.filterCategoryInt
        )

    private fun categoryDropDown() {
        val categoryAndSubCategoryMapKeys = categoryMap.keys.toTypedArray()
        val adapter = context?.let { ArrayAdapter(it, R.layout.filter_dropdown_menu_popup_item, categoryAndSubCategoryMapKeys) }
        state = 0
        var text = ""
        var catInt = -1
        filter_popup_filled_exposed_dropdown.inputType = InputType.TYPE_NULL
        filter_popup_filled_exposed_dropdown.setAdapter(adapter)

        filter_popup_filled_exposed_dropdown.setOnFocusChangeListener { v, hasFocus ->
            if(hasFocus)
                requireActivity().hideKeyboard(v)
            else
                v.clearFocus()
        }

        filter_popup_filled_exposed_dropdown.setOnItemClickListener { _, _, position, _ ->
            if (state == 0) {
                text = categoryAndSubCategoryMapKeys[position]
                catInt = position

                filter_popup_filled_exposed_dropdown.setOnDismissListener {
                    if (state == 1) {
                        state = 2
                        filter_popup_filled_exposed_dropdown.requestFocus()
                        filter_popup_filled_exposed_dropdown.showDropDown()
                    } else {
                        if (state == 2) {
                            filter_popup_filled_exposed_dropdown.setText(viewModel.filterCategoryText)
                        }
                        state = 0
                        filter_popup_filled_exposed_dropdown.setAdapter(
                            ArrayAdapter(
                                requireContext(),
                                R.layout.filter_dropdown_menu_popup_item,
                                categoryAndSubCategoryMapKeys
                            )
                        )
                        requireActivity().currentFocus?.clearFocus()
                    }
                }

                filter_popup_filled_exposed_dropdown.setAdapter(
                    ArrayAdapter(
                        requireActivity().applicationContext,
                        R.layout.filter_dropdown_menu_popup_item,
                        categoryMap[text]
                            ?: error(message = "Missing category in map")
                    )
                )
                state = 1
            } else if (state == 2) {
                state = 3
                text += " - " + filter_popup_filled_exposed_dropdown.text
                filter_popup_filled_exposed_dropdown.setText(text)
                viewModel.filterCategoryText = text
                viewModel.filterCategoryInt = catInt to position
            }
        }
    }
    override fun onRefresh() {
        swipeRefresh.isRefreshing=true
        Handler().postDelayed(Runnable {
            manageFilterQuery()
            swipeRefresh.isRefreshing = false }, 1500)
    }


}
