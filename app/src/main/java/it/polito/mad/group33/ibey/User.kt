package it.polito.mad.group33.ibey

import com.firebase.ui.firestore.SnapshotParser
import com.google.firebase.firestore.DocumentSnapshot
import it.polito.mad.group33.ibey.ui.profile.showReviews.ShowReviewsViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable

class User(
    var fullName: String = "", var nickname: String = "", var email: String =
        "", var phoneNumber: String = "", var location: Pair<Double, Double> = 91.0 to 181.0,
    var totRate:Double= 0.0, var rateCount:Long = -1) : Serializable {




    var uid: String = ""

    private val fullNameKey: String = "fullName"
    private val nicknameKey: String = "nickname"
    private val emailKey: String = "email"
    private val phoneNumberKey: String = "phoneNumber"
    private val locationNameKey: String = "location"
    private val uidKey: String = "uid"
    private val totRateKey: String = "totRate"
    private val rateCountKey: String = "rateCount"

    constructor(jsonString: String) : this() {
        val jsonObj = JSONObject(jsonString)

        if(jsonObj.has(fullNameKey))
            this.fullName = jsonObj.getString(fullNameKey)
        if(jsonObj.has(nicknameKey))
            this.nickname = jsonObj.getString(nicknameKey)
        if(jsonObj.has(emailKey))
            this.email = jsonObj.getString(emailKey)
        if(jsonObj.has(phoneNumberKey))
            this.phoneNumber = jsonObj.getString(phoneNumberKey)
        if(jsonObj.has(locationNameKey))
            this.location = Pair(
                (jsonObj.getJSONArray(locationNameKey)[0] as String).toDouble(),
                (jsonObj.getJSONArray(locationNameKey)[1] as String).toDouble())
        if(jsonObj.has(uidKey))
            this.uid = jsonObj.getString(uidKey)
        if(jsonObj.has(totRateKey))
            this.totRate = jsonObj.getDouble(totRateKey)
        if(jsonObj.has(rateCountKey))
            this.rateCount = jsonObj.getLong(rateCountKey)
    }

    override fun toString(): String {

        val jsonObj = JSONObject()
        jsonObj.put(fullNameKey, this.fullName)
        jsonObj.put(nicknameKey, this.nickname)
        jsonObj.put(emailKey, this.email)
        jsonObj.put(phoneNumberKey, this.phoneNumber)
        jsonObj.put(locationNameKey, JSONArray().put(this.location.first.toString()).put(this.location.second.toString()))
        jsonObj.put(uidKey, this.uid)
        jsonObj.put(totRateKey, this.totRate)
        jsonObj.put(rateCountKey, this.rateCount)

        return jsonObj.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is User) return false
        if (this.fullName != other.fullName             ||
            this.nickname != other.nickname             ||
            this.email    != other.email                ||
            this.phoneNumber    != other.phoneNumber    ||
            this.location != other.location             ||
            this.totRate != other.totRate               ||
            this.rateCount != other.rateCount
        ) return false
        return true
    }

    fun copy() : User {
        val u = User(this.fullName,this.nickname,this.email,this.phoneNumber,this.location,this.totRate,this.rateCount)
        u.uid=this.uid
        return u
    }

}


