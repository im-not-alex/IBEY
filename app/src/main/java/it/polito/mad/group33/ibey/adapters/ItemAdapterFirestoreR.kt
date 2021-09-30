package it.polito.mad.group33.ibey.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.storage.StorageReference
import it.polito.mad.group33.ibey.*
import it.polito.mad.group33.ibey.ui.item.itemDetail.ItemDetailsViewModel
import java.text.DecimalFormat
import java.util.*

class ItemAdapterFirestoreR(options: FirestoreRecyclerOptions<Item>, val parentFragment: Fragment, val storageRef: StorageReference,val clickAction:Int) :
    FirestoreRecyclerAdapter<Item, ItemAdapterFirestoreR.ViewHolder>(options) {
    private val viewCard = 0
    private val viewLastCard = 1
    private val viewEmptyListCard = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var l = View(parent.context)
        var lastView = false
        val layoutId = R.layout.item_recycle_card
        when(viewType)
        {
            viewCard -> l = LayoutInflater.from(parent.context)
                .inflate(layoutId, parent, false)
            viewLastCard ->
            {
                l = LayoutInflater.from(parent.context).inflate(R.layout.item_last_recycle_card, parent, false)
                l.findViewById<TextView>(R.id.idEmptyListText).visibility = View.INVISIBLE
                lastView = true
            }
            viewEmptyListCard ->
            {
                l = LayoutInflater.from(parent.context).inflate(R.layout.item_last_recycle_card, parent, false)
                lastView = true
            }
        }
        return ViewHolder(l, lastView, parentFragment, storageRef,clickAction)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, item: Item) {
            holder.bind(item)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if(position != snapshots.size)
            onBindViewHolder(holder,position,getItem(position))
    }


    override fun getItemViewType(position: Int): Int {
        return when {
            snapshots.isEmpty() -> viewEmptyListCard
            position == snapshots.size -> viewLastCard
            else -> viewCard
        }
    }

    override fun getItemCount(): Int = snapshots.size + 1


    class ViewHolder(private val v: View, lastView: Boolean = false, private val parentFragment: Fragment,val storageRef: StorageReference,val clickAction:Int): RecyclerView.ViewHolder(v)
    {
        private val title: TextView? = if(!lastView) v.findViewById(R.id.itemTitleCard) else null
        val location: TextView? = if(!lastView) v.findViewById(R.id.itemLocationCard) else null
        private val category: TextView? = if(!lastView) v.findViewById(R.id.itemCategoryCard) else null
        private val expireDate: TextView? = if(!lastView) v.findViewById(R.id.itemExpireDateCard) else null
        private val price: TextView? = if(!lastView) v.findViewById(R.id.itemPriceCard) else null
        private val editButton: Button? = if(!lastView) v.findViewById(R.id.idEditItem) else null
        private val image: ImageView? = if(!lastView) v.findViewById(R.id.itemImageCard) else null

        fun bind(item: Item){
            title!!.text = item.title
            location!!.text = getAddressFromCoordinates(item.location.first, item.location.second, parentFragment.requireContext())
            category!!.text = item.category
            expireDate!!.text = item.expiryDateString
            if (item.price >= 0.0) {   // default case, leave the box empty
                val decimalFormat = DecimalFormat.getCurrencyInstance(Locale.ITALY)
                price!!.text = decimalFormat.format(item.price)
            } else {
                price!!.visibility = View.INVISIBLE
            }

            imageSetup(item,storageRef)

            val bundle = bundleOf(singleItemKey to item.toString())
            editButton!!.setOnClickListener {
                ViewModelProvider(parentFragment.requireActivity()).get(ItemDetailsViewModel::class.java).img.value= (image?.drawable)?.toBitmap()
                NavHostFragment.findNavController(parentFragment).navigate(R.id.action_itemListFragment_to_itemEditFragment,bundle)
            }


            v.setOnClickListener {
                ViewModelProvider(parentFragment.requireActivity()).get(ItemDetailsViewModel::class.java).img.value= (image?.drawable)?.toBitmap()
                NavHostFragment.findNavController(parentFragment).navigate(clickAction, bundle) }
        }


        private fun imageSetup(item: Item, storageRef: StorageReference){
            val url = item.id + "/item_0_400x400.jpg"
            storageRef.child(url).downloadUrl.addOnSuccessListener {
                Glide.with(parentFragment).load(it).error(R.drawable.empty_picture).into(image!!)
            }.addOnFailureListener {
                Log.d("imageSetup","imageSetup ItemAdapterFirestoreR: could not download thumbnail $url")
            }
        }
    }


    override fun onChildChanged(
        type: ChangeEventType,
        snapshot: DocumentSnapshot,
        newIndex: Int,
        oldIndex: Int
    ) {
        when (type) {
            ChangeEventType.ADDED -> notifyDataSetChanged()
            ChangeEventType.CHANGED -> notifyItemChanged(newIndex)
            ChangeEventType.REMOVED -> notifyItemRemoved(oldIndex)
            ChangeEventType.MOVED -> notifyItemMoved(oldIndex, newIndex)
            else -> throw IllegalStateException("Incomplete case statement")
        }
    }


}

