package it.polito.mad.group33.ibey

import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

class Item(var id: String = "", var title: String = "", var description: String = "",
           var price: Double = -1.0, var category : String = "",
           var location: Pair<Double, Double> = 91.0 to 181.0, var expiryDate : Long = 0, var expiryDateString : String = "",
           var seller: User? = User(), var status: String = "",var statusInt:Int = 0,var categoryInt:Pair<Int,Int> = -1 to 0) : Serializable {


    constructor(id:String, title:String, description: String, price: Double, location: Pair<Double, Double>, expiryDate: Long, seller: User?, statusInt: Int, categoryInt: Pair<Int, Int>)
          :  this(id,title,description,price,"",location,expiryDate,"",seller,"",statusInt,categoryInt) {
        this.expiryDateString = if(Locale.getDefault().language=="it") {
            SimpleDateFormat("dd LLLL yyyy", Locale.ITALY).format(Date(expiryDate))
        } else {
            SimpleDateFormat("yyyy LLLL dd", Locale.UK).format(Date(expiryDate))
        }
        val category = categoryMap.keys.toTypedArray()[categoryInt.first]
        val subCategory = categoryMap[category]?.get(categoryInt.second)
        this.category = category + " - " + subCategory
        this.status= statusArray[statusInt]
    }

    private val idKey: String = "id"
    private val titleKey: String = "title"
    private val descriptionKey: String = "description"
    private val priceKey: String = "price"
    private val categoryKey: String = "category"
    private val locationKey: String = "location"
    private val expiryDateKey: String = "expiryDate"
    private val expiryDateStringKey: String = "expiryDateString"
    private val statusKey: String = "status"
    private val statusIntKey: String = "statusInt"
    private val categoryIntKey: String = "categoryInt"
    private val sellerKey: String = "seller"


    constructor(jsonString: String) : this() {
        val jsonObj = JSONObject(jsonString)
        if(jsonObj.has(idKey))
            this.id = jsonObj.getString(idKey)
        if(jsonObj.has(titleKey))
            this.title = jsonObj.getString(titleKey)
        if(jsonObj.has(descriptionKey))
        if(jsonObj.has(priceKey))
            this.price = jsonObj.getDouble(priceKey)
        if(jsonObj.has(categoryKey))
            this.category = jsonObj.getString(categoryKey)
        this.description = jsonObj.getString(descriptionKey)
        if(jsonObj.has(locationKey))
            this.location = Pair(
                (jsonObj.getJSONArray(locationKey)[0] as String).toDouble(),
                (jsonObj.getJSONArray(locationKey)[1] as String).toDouble())
        if(jsonObj.has(expiryDateKey))
            this.expiryDate = jsonObj.getLong(expiryDateKey)
        if(jsonObj.has(expiryDateStringKey))
            this.expiryDateString = jsonObj.getString(expiryDateStringKey)
        if(jsonObj.has(statusKey))
            this.status = jsonObj.getString(statusKey)
        if(jsonObj.has(statusIntKey))
            this.statusInt = jsonObj.getInt(statusIntKey)
        if(jsonObj.has(categoryIntKey)){
            this.categoryInt = Pair(
                jsonObj.getJSONArray(categoryIntKey)[0] as Int,
                jsonObj.getJSONArray(categoryIntKey)[1] as Int)
        }
        if(jsonObj.has(sellerKey))
            this.seller = User(jsonObj.getString(sellerKey))
    }

    override fun toString(): String {
        val jsonObj = JSONObject()
        jsonObj.put(idKey, this.id)
        jsonObj.put(titleKey, this.title)
        jsonObj.put(descriptionKey, this.description)
        jsonObj.put(priceKey, this.price)
        jsonObj.put(categoryKey, this.category)
        jsonObj.put(locationKey, JSONArray().put(this.location.first.toString()).put(this.location.second.toString()))
        jsonObj.put(expiryDateKey, this.expiryDate)
        jsonObj.put(expiryDateStringKey, this.expiryDateString)
        jsonObj.put(statusKey, this.status)
        jsonObj.put(statusIntKey, this.statusInt)
        jsonObj.put(categoryIntKey, JSONArray().put(this.categoryInt.first).put(this.categoryInt.second))
        jsonObj.put(sellerKey, this.seller)

        return jsonObj.toString()
    }

    fun copy() : Item {
        return Item(this.id,this.title,this.description,this.price,this.category,this.location,this.expiryDate,this.expiryDateString, this.seller, this.status,this.statusInt,this.categoryInt)
    }
}