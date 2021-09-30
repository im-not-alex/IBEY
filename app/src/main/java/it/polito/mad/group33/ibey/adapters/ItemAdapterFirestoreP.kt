package it.polito.mad.group33.ibey.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firebase.ui.firestore.paging.FirestorePagingAdapter
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.google.firebase.storage.StorageReference
import it.polito.mad.group33.ibey.*
import java.text.DecimalFormat


class ItemAdapterFirestoreP(val options: FirestorePagingOptions<Item>, val parentFragment: Fragment, val onSaleList: Boolean = false, val myuser: User, val storageRef: StorageReference) :
    FirestorePagingAdapter<Item, ItemAdapterFirestoreP.ViewHolder>(options) {
    private val viewCard = 0
    private val viewLastCard = 1
    private val viewEmptyListCard = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var l = View(parent.context)
        var lastView = false
        val layoutId = if(onSaleList) R.layout.item_common_recycle_card else R.layout.item_recycle_card
        when(viewType)
        {
            viewCard -> {
                l = LayoutInflater.from(parent.context)
                    .inflate(layoutId, parent, false)
            }
            viewLastCard ->
            {
                l = LayoutInflater.from(parent.context).inflate(R.layout.item_last_recycle_card, parent, false)
                l.findViewById<TextView>(R.id.idEmptyListText).visibility = View.INVISIBLE
                lastView = true
            }
            viewEmptyListCard ->
            {
                l = LayoutInflater.from(parent.context).inflate(R.layout.item_last_recycle_card, parent, false)
                if(onSaleList)
                    l.findViewById<TextView>(R.id.idEmptyListText).text = parentFragment.requireActivity().getString(
                        R.string.emptyCommonList
                    )
                lastView = true
            }
        }
        return ViewHolder(
            l,
            lastView,
            parentFragment,
            onSaleList,
            storageRef
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, item: Item){
        Log.d("Bind",item.toString())
        if(item.seller?.uid==myuser.uid){
            holder.bind(null)
        }else
            holder.bind(item)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if(currentList!=null && position >0){
            val snapshot = getItem(position-1)!!
            onBindViewHolder(holder,position-1,options.parser.parseSnapshot(snapshot))
        }
    }


    override fun getItemViewType(position: Int): Int {
        return when {
            currentList == null || currentList?.isEmpty()!! -> viewEmptyListCard
            position == 0 -> viewLastCard
            else -> viewCard
        }
    }


    override fun getItemCount(): Int = (currentList?.size ?: 0) +1


    class ViewHolder(private val v: View, lastView: Boolean = false, private val parentFragment: Fragment, private val onSaleList: Boolean = false,val storageRef: StorageReference): RecyclerView.ViewHolder(v)
    {
        private val title: TextView? = if(!lastView) v.findViewById(R.id.itemTitleCard) else null
        val location: TextView? = if(!lastView) v.findViewById(R.id.itemLocationCard) else null
        private val category: TextView? = if(!lastView) v.findViewById(R.id.itemCategoryCard) else null
        private val expireDate: TextView? = if(!lastView) v.findViewById(R.id.itemExpireDateCard) else null
        private val price: TextView? = if(!lastView) v.findViewById(R.id.itemPriceCard) else null
        private val editButton: Button? = if(!lastView) v.findViewById(R.id.idEditItem) else null
        private val image: ImageView? = if(!lastView) v.findViewById(R.id.itemImageCard) else null

        private val sellerNickname: TextView? = if(!lastView && onSaleList) v.findViewById(
            R.id.itemSellerNickname
        ) else null

        fun bind(item: Item?){
            if(item!=null){
                title!!.text = item.title
                location!!.text = getAddressFromCoordinates(item.location.first, item.location.second, parentFragment.requireContext())
                category!!.text = item.category
                expireDate!!.text = item.expiryDateString
                if (item.price >= 0.0) {   // default case, leave the box empty
                    val decimalFormat = DecimalFormat.getCurrencyInstance(
                        getCurrentLocale(
                            parentFragment.requireContext()
                        )
                    )
                    price!!.text = decimalFormat.format(item.price)
                } else {
                    price!!.visibility = View.INVISIBLE
                }

                imageSetup(item,storageRef)

                val bundle = bundleOf(singleItemKey to item.toString())
                sellerNickname!!.text = item.seller?.nickname?: ""
                val action =
                    R.id.action_onSaleListFragment_to_otherItemDetailsFragment
                v.setOnClickListener { NavHostFragment.findNavController(parentFragment).navigate(action, bundle) }
            } else{
                v.layoutParams.height=0  // TODO METTERLI!\
                v.layoutParams = ViewGroup.MarginLayoutParams(0,0)
            }
        }

        private fun imageSetup(item: Item, storageRef: StorageReference){
            val url = item.seller!!.uid + "/" + item.id + "/item_0_400x400.jpg"
            storageRef.child(url).downloadUrl.addOnSuccessListener {
                Glide.with(parentFragment).load(it).error(R.drawable.empty_picture).into(image!!)
            }.addOnFailureListener {
                Log.d("imageSetup","imageSetup ItemAdapterFirestoreR: could not download thumbnail $url")
            }
        }

    }


}