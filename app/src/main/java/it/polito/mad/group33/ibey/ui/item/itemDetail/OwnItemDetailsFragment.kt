package it.polito.mad.group33.ibey.ui.item.itemDetail

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.beloo.widget.chipslayoutmanager.ChipsLayoutManager
import com.beloo.widget.chipslayoutmanager.SpacingItemDecoration
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.EventListener
import it.polito.mad.group33.ibey.*
import it.polito.mad.group33.ibey.repository.firebaseRepository
import kotlinx.android.synthetic.main.item_details_fragment.*
import kotlinx.android.synthetic.main.item_show_fields.*
import kotlinx.android.synthetic.main.item_show_interests.*
import kotlinx.android.synthetic.main.see_buyer_vote.*
import kotlinx.android.synthetic.main.user_chip.view.*
import java.io.Serializable
import java.text.DecimalFormat

class OwnItemDetailsFragment : ItemDetailsFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(ItemDetailsViewModel::class.java)
        val bundle = this.arguments
        if (bundle != null) {
            val bundleItem=Item(bundle.getString(singleItemKey)!!)
            if(viewModel.myItem!=null && viewModel.myItem?.id!=bundleItem.id)
                viewModel.clear()
            viewModel.myItem=bundleItem
        }
        return inflater.inflate(R.layout.item_details_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bundle = this.arguments
        val mFabOpenAnim : Animation = AnimationUtils.loadAnimation(requireActivity(), R.anim.fab_open)
        val mFabCloseAnim : Animation = AnimationUtils.loadAnimation(requireActivity(), R.anim.fab_close)

        if (bundle?.getBoolean(newFlagKey)==true){ // item appena creato, notifico l'utente
            id_edit.visibility = View.GONE
            item_show_interests.visibility = View.GONE
            Snackbar.make(app_bar_main, getString(R.string.toastItemAdded), Snackbar.LENGTH_LONG).show()
            return
        }
        loadInterests()
        if(viewModel.myItem?.statusInt == 1){    // status venduto
            id_edit.visibility = View.GONE
            item_show_interests_button.setOnClickListener{
                openOrCloseRecycler()
            }
            viewModel.reviewPath.observe(viewLifecycleOwner,Observer{
                if(it!=null && (viewModel.myItem?.statusInt == 1)){
                    if(it==""){ // still no review
                        if (viewModel.buyerUid.value!=null && viewModel.buyerUid.value=="other") {
                            show_buyer_review.visibility = View.GONE
                        }else{
                            show_buyer_review.visibility = View.VISIBLE
                        }
                    }else{  // review to be downloaded
                        loadReview()
                    }
                }else
                    show_buyer_review.visibility = View.GONE
            })

            viewModel.buyerUid.observe(viewLifecycleOwner, Observer {
                if (it!=null && it=="other"){
                    item_show_interests_button.text = requireContext().resources.getString(R.string.soldUser, "Other")
                    bought_icon.visibility=View.VISIBLE
                    item_show_interests_button.isClickable=true
                    id_edit.visibility=View.GONE
                    show_buyer_review.visibility = View.GONE
                }
            })
        }
        else{
            // listener button edit
            id_edit.setOnClickListener {
                if(viewModel.isIdEditOpen){
                    id_sell!!.startAnimation(mFabCloseAnim)
                    id_sell_text!!.startAnimation(mFabCloseAnim)
                    edit_item!!.startAnimation(mFabCloseAnim)
                    edit_item_text!!.startAnimation(mFabCloseAnim)
                    id_edit.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.ic_menu_edit_or_buy))
                    viewModel.isIdEditOpen = false


                } else {
                    if(viewModel.isIdSellOpen){
                        closeSellPanel()

                    } else {
                        id_sell!!.startAnimation(mFabOpenAnim)
                        id_sell_text!!.startAnimation(mFabOpenAnim)
                        edit_item!!.startAnimation(mFabOpenAnim)
                        edit_item_text!!.startAnimation(mFabOpenAnim)
                        id_edit.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.cross_cancel_mini))
                        viewModel.isIdEditOpen = true

                    }

                }
            }

            edit_item.setOnClickListener {
                findNavController().navigate(R.id.action_itemDetailsFragment_to_itemEditFragment, bundle)
                viewModel.isIdEditOpen = false
            }

            id_sell.setOnClickListener{
                openSellPanel()
            }

            // listener button open interested people
            item_show_interests_button.setOnClickListener{openOrCloseRecycler()}

        }
        viewModel.usersInterested.observe(viewLifecycleOwner,Observer {
            if(viewModel.buyerUid.value!=null && viewModel.buyerUid.value!="") {
                val nicksold=viewModel.interested[viewModel.buyerUid.value!!]?.nickname?: ""
                item_show_interests_button.text = requireContext().resources.getString(R.string.soldUser, nicksold)
                item_show_interests_button.isClickable=true
                bought_icon.visibility=View.VISIBLE
                id_edit.visibility=View.GONE // TODO UNCOMMENT fatto, check che non crashi
            }
            else if(it.size==1){
                item_show_interests_button.text= requireContext().resources.getString(R.string.interest_single,it.size)
                item_show_interests_button.isClickable=true
            }
            else{
                if(it.size == 0){
                    item_show_interests_button.isClickable = false
                    item_show_interests_button.icon = ContextCompat.getDrawable(requireActivity(), R.drawable.ic_keyboard_arrow_right)
                    interestsRecyclerView.visibility=View.GONE
                }
                item_show_interests_button.text= requireContext().resources.getString(R.string.interest,it.size)
            }
        })
    }

    fun openSellPanel(){
        val mFabCloseAnim : Animation = AnimationUtils.loadAnimation(requireActivity(), R.anim.fab_close)
        val mFabOpenAnim : Animation = AnimationUtils.loadAnimation(requireActivity(), R.anim.fab_open)
        id_sell_to_other.visibility = View.VISIBLE
        id_sell_to_other.startAnimation(mFabOpenAnim)
        id_edit.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.cross_cancel_mini))
        viewModel.isIdSellOpen = true
        id_sell!!.startAnimation(mFabCloseAnim)
        id_sell_text!!.startAnimation(mFabCloseAnim)
        edit_item!!.startAnimation(mFabCloseAnim)
        edit_item_text!!.startAnimation(mFabCloseAnim)

        viewModel.isIdEditOpen = false

        interestsRecyclerView.visibility = View.VISIBLE
        item_show_interests_button.icon = ContextCompat.getDrawable(requireActivity(), R.drawable.ic_keyboard_arrow_down)
        item_show_interests_button.text = requireContext().resources.getString(R.string.sellUsers)
       id_sell_to_other.setOnClickListener {
           firebaseRepository.makeBuy("other", viewModel.myItem!!.id)
           closeSellPanel()
           id_edit.visibility = View.INVISIBLE
       }
    }

    fun closeSellPanel(){
        id_edit.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.ic_menu_edit_or_buy))
        viewModel.isIdSellOpen = false
        item_show_interests_button.icon = ContextCompat.getDrawable(requireActivity(), R.drawable.ic_keyboard_arrow_right)
        interestsRecyclerView.visibility = View.GONE
        id_sell_to_other.visibility = View.GONE

        // TODO Mostare fisso venduto a pippo basandosi su buyer uid
    }

    override fun setItemInformation(myItem: Item)
    {
        text_item_title!!.setText(myItem.title)
        if(myItem.description!="")
            text_item_description!!.setText(myItem.description)
        else
            id_item_description.visibility=View.GONE
        if (myItem.price!=-1.0){ // default case, leave the box empty
            val decimalFormat = DecimalFormat.getCurrencyInstance(getCurrentLocale(this.requireContext()))
            text_item_price!!.setText(decimalFormat.format(myItem.price))
        }
        text_item_category!!.setText(myItem.category)
        text_item_date!!.setText(myItem.expiryDateString)

        setHiddenSwitchStatus(myItem.statusInt, showHiddenSwitch, statusHiddenLayout)

        // boolean modificato da Item Edit, modifica la bitmap lui, NON deve essere scaricata: problemi di concorrenza
        if(!viewModel.imgModified)
            getItemImage("item_0.jpg","Users/"+myItem.seller!!.uid + "/" + myItem.id)   // seller uid corrisponde generico: tanto per proprio user quanto per altro seller
        else
            viewModel.imgModified = false
    }

    private fun loadInterests(){
        // snapshotlistner sull'item --> quando arriva confronto con interests in locale e add al view model
        viewModel.myItem?.id?.let { viewModel.retrieveInterests(it) }

        // creare Lista di utenti da visualizzare partendo dalla lista di id
        viewModel.adapter=ChipsAdapter(this)
        interestsRecyclerView.adapter = viewModel.adapter

        //interestsRecyclerView.adapter.notify


        val chipsLayoutManager = ChipsLayoutManager.newBuilder(context)
            //set vertical gravity for all items in a row. Default = Gravity.CENTER_VERTICAL
            .setChildGravity(Gravity.LEFT)
            .setScrollingEnabled(true)
            //set gravity resolver where you can determine gravity for item in position.
            //This method have priority over previous one
            .setGravityResolver { Gravity.NO_GRAVITY}
            .setOrientation(ChipsLayoutManager.HORIZONTAL)
            //row strategy for views in completed row, could be
            //STRATEGY_DEFAULT, STRATEGY_FILL_VIEW, STRATEGY_FILL_SPACE or STRATEGY_CENTER
            .setRowStrategy(ChipsLayoutManager.STRATEGY_DEFAULT)
            .build()

        interestsRecyclerView.layoutManager = chipsLayoutManager

        interestsRecyclerView.addItemDecoration(
            SpacingItemDecoration(
                resources.getDimensionPixelOffset(R.dimen.chips_horizontal_margin),
                resources.getDimensionPixelOffset(R.dimen.chips_vertical_margin)
            )
        )
    }

   inner class ChipsAdapter(val parentFragment: Fragment) : RecyclerView.Adapter<ChipsAdapter.ChipViewHolder>(){
        private val viewmodel=ViewModelProvider(parentFragment.requireActivity()).get(ItemDetailsViewModel::class.java)

       override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
           val v = LayoutInflater.from(parent.context).inflate(R.layout.user_chip, parent, false)
           return ChipViewHolder(v)
       }

       override fun getItemCount() = viewmodel.usersInterested.value!!.size

       override fun onBindViewHolder(holder: ChipViewHolder, position: Int) = holder.bind(viewModel.usersInterested.value!![position])


       inner class ChipViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

           @SuppressLint("SetTextI18n")
           fun bind(uid: String) {
               with(itemView) {

                   userChip.text = viewmodel.interested[uid]!!.nickname
                   val bundle = bundleOf(singleUserKey to viewmodel.interested[uid].toString())
                   userChip.setOnClickListener {
                       if (viewmodel.isIdSellOpen) { // se mi trovo in modalita vendita
                           viewmodel.myItem?.id?.let { itemId ->
                               firebaseRepository.makeBuy(uid, itemId)
                           }
                           closeSellPanel()
                           id_edit.visibility = View.INVISIBLE
                       } else {
                           findNavController(parentFragment).navigate(
                               R.id.action_OwnItemDetails_to_otherProfileFragment,
                               bundle
                           )
                       }
                   }
                   if (uid == viewmodel.buyerUid.value)    // golden buyer
                       userChip.setChipBackgroundColorResource(R.color.secondaryDarkColor)
                   imageSetup(uid, userChip)
                   val mFabOpenAnim: Animation = AnimationUtils.loadAnimation(
                       parentFragment.requireActivity(),
                       R.anim.fab_open
                   )
                   userChip.startAnimation(mFabOpenAnim)
               }
           }

           private fun imageSetup(id: String, chip: Chip) {
               val url = "profile_0_36x36.jpg"
               firebaseRepository.getImg(url, "Users/$id").addOnSuccessListener {
                   Thread(Runnable {
                       Glide.with(parentFragment).asDrawable().load(it).circleCrop()
                           .placeholder(R.drawable.empty_picture).error(R.drawable.empty_picture)
                           .into(object : CustomTarget<Drawable>() {
                               override fun onLoadCleared(placeholder: Drawable?) {}
                               override fun onResourceReady(
                                   resource: Drawable,
                                   transition: Transition<in Drawable>?
                               ) {
                                   chip.chipIcon = resource
                                   Log.d("imageSetup", "OK chip: $url")
                               }
                           })
                   }).start()
               }.addOnFailureListener {
                   Log.d("imageSetup", "imageSetup Chips: could not download thumbnail $url")
               }
           }

       }

    }

    private fun loadReview() {
        class Review(val rate:Double,val description:String =""): Serializable
        var buyerReview:Review
        firebaseRepository.loadReview(viewModel.reviewPath.value!!).addOnSuccessListener {
            buyerReview = Review(it.getDouble("rate")?:0.0,it.getString("description")?:"")
            rating_bar.rating = buyerReview.rate.toFloat()
            title_review.text = getString(R.string.see_rate_title)
            show_buyer_review.visibility=View.VISIBLE
            if(buyerReview.description!=""){
                review.text = "\u201C"+buyerReview.description+"\u201D"
                review.visibility=View.VISIBLE
                }
        }.addOnFailureListener {
            Log.e("OwnItemDetailsFragment","loadReview: "+it.message)
        }
    }


    private fun openOrCloseRecycler(){
        if(interestsRecyclerView.isVisible) {
            item_show_interests_button.icon = ContextCompat.getDrawable(requireActivity(), R.drawable.ic_keyboard_arrow_right)
            interestsRecyclerView.visibility=View.GONE
        }else{
                item_show_interests_button.icon = ContextCompat.getDrawable(requireActivity(), R.drawable.ic_keyboard_arrow_down)
                interestsRecyclerView.visibility=View.VISIBLE
        }
    }
}