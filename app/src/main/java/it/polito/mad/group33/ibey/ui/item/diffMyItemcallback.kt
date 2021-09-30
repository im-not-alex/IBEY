package it.polito.mad.group33.ibey.ui.item

import androidx.recyclerview.widget.DiffUtil

class diffMyItemcallback(val old:List<String>,val new:List<String>) : DiffUtil.Callback() {
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return old.containsAll(new) && (old.size == new.size)
    }

    override fun getOldListSize(): Int {
        return old.size
    }

    override fun getNewListSize(): Int {
        return new.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return old[oldItemPosition] == new[newItemPosition]
    }

}