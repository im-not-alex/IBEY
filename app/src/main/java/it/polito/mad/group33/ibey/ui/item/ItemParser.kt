package it.polito.mad.group33.ibey.ui.item

import android.content.Context
import android.util.Log
import com.firebase.ui.firestore.SnapshotParser
import com.google.firebase.firestore.DocumentSnapshot
import it.polito.mad.group33.ibey.*

class ItemParser(): SnapshotParser<Item> {
    override fun parseSnapshot(snapshot: DocumentSnapshot): Item {
        @Suppress("UNCHECKED_CAST") val categoryHashmap: HashMap<String,Long> = (snapshot.get("category") as HashMap<String, Long>)
        val categoryInt = categoryHashmap["first"]?.toInt()?: 0
        val subCategoryInt = categoryHashmap["second"]?.toInt()?: 0

        val seller = User(nickname= snapshot.getString("sellerNickname")?: "").apply {  uid = snapshot.getString("sellerUid")?: ""}

        return Item(
            snapshot.id,
            snapshot.getString("title")?: "",
            snapshot.getString("description")?: "",
            snapshot.getDouble("price")?: 0.0,
            getLocationFromDocument(snapshot),
            snapshot.getLong("expiryDate")?: 0,
            seller,
            (snapshot.getLong("status")?: 0).toInt(),
            Pair(categoryInt,subCategoryInt)
        )
    }
}