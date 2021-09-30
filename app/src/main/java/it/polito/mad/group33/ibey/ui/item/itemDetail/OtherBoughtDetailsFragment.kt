package it.polito.mad.group33.ibey.ui.item.itemDetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import it.polito.mad.group33.ibey.*
import it.polito.mad.group33.ibey.ui.profile.UserViewModel
import kotlinx.android.synthetic.main.item_details_fragment.*
import kotlinx.android.synthetic.main.item_rate_seller.*
import kotlinx.android.synthetic.main.item_show_fields.*


class OtherBoughtDetailsFragment : OtherItemDetailsFragment() {
    private lateinit var boughtViewModel: BoughtViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //super.onCreateView(inflater, container, savedInstanceState)
        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)
        viewModel = ViewModelProvider(this).get(ItemDetailsViewModel::class.java)
        boughtViewModel = ViewModelProvider(this).get(BoughtViewModel::class.java)
        val bundle = this.arguments
        viewModel.myItem= Item(bundle?.getString(singleItemKey)!!)
        boughtViewModel.transactionId= bundle.getString(transactionIdKey) ?:""
        viewModel.interest = false

        val view = inflater.inflate(R.layout.item_bought_details_public_fragment, container, false)
        mapView = view.findViewById(R.id.idShowItemLocationMap)
        mapView.onCreate(savedInstanceState)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        id_edit.visibility = View.GONE  // hide FAB button
        show_review.visibility = View.GONE
        text_item_date.visibility = View.GONE    // remove data scadenza vedendo oggetto acquistato
                                                // TODO farlo diventare data di acquisto?
        boughtViewModel.loadTransaction()

        boughtViewModel.reviewDone.observe(viewLifecycleOwner, Observer {
            when {
                it == null -> {
                    show_review.visibility = View.GONE
                }
                it -> {
                    show_review.visibility = View.VISIBLE
                    setReviewDone()
                }
                else -> {
                    show_review.visibility = View.VISIBLE
                    rating_bar.setOnRatingBarChangeListener { ratingBar, rating, fromUser ->
                        if (!boughtViewModel.dialogOpen) {
                            firstSetRating(rating)
                            // open the dialog
                            RateDialog(requireActivity(), boughtViewModel, viewModel).show()
                        }
                    }
                }
            }
        })

        boughtViewModel.actualRate.observe(viewLifecycleOwner, Observer {
            boughtViewModel.dialogOpen=true
            rating_bar.rating=it
            boughtViewModel.dialogOpen=false
        })

        boughtViewModel.comment.observe(viewLifecycleOwner, Observer {
            review.text = "\u201C${boughtViewModel.comment.value}\u201D"
        })

    }

    override fun getNavAction(): Int {
        return R.id.action_otherBoughtDetailsFragment_to_otherProfileFragment
    }

    private fun firstSetRating(rating: Float) {
        boughtViewModel.actualRate.value = rating
        // update on change inside the
    }

    private fun setReviewDone(){
        rating_bar.onRatingBarChangeListener = null
        //boughtViewModel.dialogOpen=true
        rating_bar.setIsIndicator(true)
        title.visibility = View.GONE
        if(boughtViewModel.comment.value!="")
            review.visibility = View.VISIBLE
    }
}