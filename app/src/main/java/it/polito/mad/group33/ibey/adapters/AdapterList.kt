package it.polito.mad.group33.ibey.adapters

import android.graphics.Color
import android.graphics.ColorFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat.setBackgroundTintList
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import com.google.firebase.storage.StorageReference
import it.polito.mad.group33.ibey.*
import it.polito.mad.group33.ibey.R
import it.polito.mad.group33.ibey.repository.firebaseRepository
import it.polito.mad.group33.ibey.ui.item.ItemParser
import it.polito.mad.group33.ibey.ui.item.boughtItemsListFragment.BoughtItemsListFragment
import it.polito.mad.group33.ibey.ui.item.boughtItemsListFragment.BoughtItemsListViewModel
import it.polito.mad.group33.ibey.ui.item.itemDetail.BoughtViewModel
import it.polito.mad.group33.ibey.ui.item.itemDetail.ItemDetailsViewModel
import it.polito.mad.group33.ibey.ui.profile.UserViewModel
import java.text.DecimalFormat
import java.util.*


class AdapterList(val parentFragment: Fragment, val layoutCardId:Int, val stringLastCardId:Int, val storageRef: StorageReference, val clickNavAction:Int,val editNavAction:Int?=null, vClass: Class<out BaseItemsRecycleViewModel>)
    : RecyclerView.Adapter<AdapterList.ViewHolder>() {
    private val viewModel= ViewModelProvider(parentFragment).get(vClass)
    private val viewCard = 0
    private val viewLastCard = 1
    private val viewEmptyListCard = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var l = View(parent.context)
        var lastView = false
        when(viewType)
        {
            viewCard -> l = LayoutInflater.from(parent.context).inflate(layoutCardId, parent, false)
            viewLastCard ->{
                l = LayoutInflater.from(parent.context).inflate(R.layout.item_last_recycle_card, parent, false)
                l.findViewById<TextView>(R.id.idEmptyListText).visibility = View.INVISIBLE
                lastView = true
            }
            viewEmptyListCard ->{
                l = LayoutInflater.from(parent.context).inflate(R.layout.item_last_recycle_card, parent, false)
                l.findViewById<TextView>(R.id.idEmptyListText).text= parentFragment.requireContext().resources.getString(stringLastCardId)
                lastView = true
            }
        }
        return ViewHolder(l, lastView,parentFragment,storageRef,clickNavAction,editNavAction)
    }

    override fun getItemCount(): Int = viewModel.objectsList.size  // TODO +1 se con last card

    override fun getItemViewType(position: Int): Int {
        return when {
            itemCount<=1 -> viewEmptyListCard
            position == 0 -> viewLastCard
            else -> viewCard
        }
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int){
        if(position > 0){
            holder.loadItem()
        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        holder.removeListener()
        super.onViewDetachedFromWindow(holder)
    }

    inner class ViewHolder(val v: View,lastView: Boolean = false, private val parentFragment: Fragment,val storageRef: StorageReference,val clickNavAction:Int,val editNavAction:Int?=null)
        : RecyclerView.ViewHolder(v) {
        private val title: TextView? = if(!lastView) v.findViewById(R.id.itemTitleCard) else null
        val location: TextView? = if(!lastView) v.findViewById(R.id.itemLocationCard) else null
        private val category: TextView? = if(!lastView) v.findViewById(R.id.itemCategoryCard) else null
        private val expireDate: TextView? = if(!lastView) v.findViewById(R.id.itemExpireDateCard) else null
        private val price: TextView? = if(!lastView) v.findViewById(R.id.itemPriceCard) else null
        private val editButton: ImageButton? = if(!lastView) v.findViewById(R.id.idEditItem) else null
        private val image: ImageView? = if(!lastView) v.findViewById(R.id.itemImageCard) else null
        private val itemListCard: MaterialCardView? = if(!lastView) v.findViewById(R.id.itemListCard) else null
        private var listenerRegistration: ListenerRegistration? = null
        private lateinit var id:String

        fun bind(item: Item){
            title!!.text = item.title
            location!!.text = parentFragment.context?.let {getAddressFromCoordinates(item.location.first, item.location.second,it)}
            category!!.text = item.category
            val daysleft =  (( item.expiryDate -System.currentTimeMillis() )/(60*60*24*1000)).toInt()
            Log.d("boh",item.expiryDate.toString())
            expireDate!!.text = if(daysleft>1) parentFragment.requireContext().getString(R.string.days,daysleft) else if(daysleft==1) parentFragment.requireContext().getString(R.string.day,daysleft) else parentFragment.requireContext().getString(R.string.days_today)
            if (item.price >= 0.0) {   // default case, leave the box empty
                val decimalFormat = DecimalFormat.getCurrencyInstance(Locale.ITALY)
                price!!.text = decimalFormat.format(item.price)
            } else {
                price!!.visibility = View.INVISIBLE
            }
            if(parentFragment is BoughtItemsListFragment)
                expireDate.visibility=View.INVISIBLE
            imageSetup(item,storageRef)
            val bundle = if(clickNavAction!=R.id.action_boughtItemsListFragment_to_otherBoughtDetailsFragment)
                bundleOf(singleItemKey to item.toString())
            else
                bundleOf(singleItemKey to item.toString(),transactionIdKey to (viewModel as BoughtItemsListViewModel).idToTransaction[item.id])

            if(editNavAction!=null)
                if(editNavAction==R.id.action_itemListFragment_to_itemEditFragment && item.statusInt==1){
                    editButton?.setImageResource(R.drawable.ic_sold_vertical)
                    editButton?.isEnabled=false
                    editButton?.setColorFilter(ContextCompat.getColor(parentFragment.requireContext(),R.color.red_sold),android.graphics.PorterDuff.Mode.SRC_IN)
                }else{
                    editButton?.setOnClickListener {
                        ViewModelProvider(parentFragment.requireActivity()).get(ItemDetailsViewModel::class.java).img.value= (image?.drawable)?.toBitmap()
                        NavHostFragment.findNavController(parentFragment).navigate(editNavAction,bundle)
                    }
                }
            else
                editButton?.visibility=View.GONE

            itemListCard?.setOnClickListener {
                ViewModelProvider(parentFragment.requireActivity()).get(ItemDetailsViewModel::class.java).img.value= (image?.drawable)?.toBitmap()
                NavHostFragment.findNavController(parentFragment).navigate(clickNavAction, bundle) }
        }

        private fun imageSetup(item: Item, storageRef: StorageReference){
            val url = item.seller!!.uid+"/"+item.id + "/item_0_400x400.jpg"
            storageRef.child(url).downloadUrl.addOnSuccessListener {
                Glide.with(parentFragment).load(it).error(R.drawable.empty_picture).into(image!!)
            }.addOnFailureListener {
                Log.d("imageSetup","imageSetup ItemAdapterFirestoreR: could not download thumbnail $url")
            }
        }

        fun loadItem(){
            id=viewModel.objectsList[adapterPosition]
            val elem = viewModel.myItemsObj[id]
            if(elem!=null){
                bind(elem)
            }
            else {
                listenerRegistration = firebaseRepository.loadItem(id)
                    .addSnapshotListener(EventListener { document, e ->
                        if (e != null) {
                            Log.w("error", "Listen AdapterList failed.", e)
                            // TODO in caso di errore elemento non viene caricato
                            return@EventListener
                        }
                        if (document != null) {
                            viewModel.myItemsObj[id] = ItemParser().parseSnapshot(document)
                            bind(viewModel.myItemsObj[id]!!) // TODO check
                        }
                    })
            }
        }
        fun removeListener(){
            if(this::id.isInitialized)
                viewModel.myItemsObj.remove(id)
            listenerRegistration?.remove()
        }


    }
}