package it.polito.mad.group33.ibey.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import it.polito.mad.group33.ibey.*
import java.io.File

class ItemAdapter(val items: List<String>, val parentFragment: Fragment, val onSaleList: Boolean = false) :  RecyclerView.Adapter<ItemAdapter.ViewHolder>(){
    private val viewCard = 0
    private val viewLastCard = 1
    private val viewEmptyListCard = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var l = View(parent.context)

        l = LayoutInflater.from(parent.context).inflate(R.layout.user_chip, parent, false)
//        when(viewType)
//        {
//            viewCard -> l = LayoutInflater.from(parent.context)
//                .inflate(layoutId, parent, false)
//            viewLastCard ->
//            {
//                l = LayoutInflater.from(parent.context).inflate(R.layout.item_last_recycle_card, parent, false)
//                l.findViewById<TextView>(R.id.idEmptyListText).visibility = View.INVISIBLE
//                lastView = true
//            }
//            viewEmptyListCard ->
//            {
//                l = LayoutInflater.from(parent.context).inflate(R.layout.item_last_recycle_card, parent, false)
//                if(onSaleList)
//                    l.findViewById<TextView>(R.id.idEmptyListText).text = parentFragment.requireActivity().getString(R.string.emptyCommonList)
//                lastView = true
//            }
//        }
        return ViewHolder(
            l,
            parentFragment,
            onSaleList
        )
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            items.isEmpty() -> viewEmptyListCard
            position == items.size -> viewLastCard
            else -> viewCard
        }
    }

    override fun getItemCount(): Int = items.size + 1

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if(position != items.size)
            holder.bind(items[position])
    }

    class ViewHolder(private val v: View, private val parentFragment: Fragment, private val onSaleList: Boolean = false): RecyclerView.ViewHolder(v)
    {
        private val text: TextView? = v.findViewById(R.id.itemTitleCard)
        private val image: ImageView? = v.findViewById(R.id.itemImageCard)

        fun bind(item: String){

            // chips.text = item

//            title!!.text = item.title
//            location!!.text = item.location
//            category!!.text = item.category
//            expireDate!!.text = item.expiryDateString
//            if (item.price >= 0.0) {   // default case, leave the box empty
//                val decimalFormat = DecimalFormat.getCurrencyInstance(getCurrentLocale(parentFragment.requireContext()))
//                price!!.text = decimalFormat.format(item.price)
//            } else {
//                price!!.visibility = View.INVISIBLE
//            }
//
//            imageSetup(item.id)
//
//            val bundle = bundleOf(singleItemKey to item.toString())
//            var action = -1
//            if(onSaleList) {
//                sellerNickname!!.text = item.seller?.nickname?: ""
//                action = R.id.action_onSaleListFragment_to_itemDetailsFragment
//            } else {
//                action = R.id.action_itemListFragment_to_itemDetailsFragment
//                editButton!!.setOnClickListener {
//                    findNavController(parentFragment).navigate(
//                        R.id.action_itemListFragment_to_itemEditFragment,
//                        bundle
//                    )
//                }
//            }
//
//            v.setOnClickListener { findNavController(parentFragment).navigate(action, bundle) }
        }

        private fun imageSetup(itemId: String) {
            val imageFileName = "/" + itemFolderName + "/" + itemPicNamePrefix + itemId + itemPicNameExtension
            val imagePicPath = parentFragment.requireActivity().filesDir.absolutePath +  imageFileName
            if (File(parentFragment.requireActivity().filesDir, imageFileName).exists()) {
                //image!!.setImageBitmap(BitmapFactory.decodeFile(imagePicPath))
                image!!.setImageBitmap(
                    getResizedBitmap(
                        BitmapFactory.decodeFile(imagePicPath)
                    )
                )
            }
        }
    }
}