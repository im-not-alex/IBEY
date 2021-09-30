package it.polito.mad.group33.ibey

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.location.Geocoder
import android.os.Build
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.Marker
import com.google.android.libraries.maps.model.MarkerOptions
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.android.synthetic.main.item_show_fields.*
import java.util.*

const val profileKey: String = "profileKey"
const val new_profileKey: String = "new_profileKey"
const val requestCodeEditProfile: Int = 1
const val profilePicName:String = "profile_pic.jpg"
const val tempPic:String = "temp_pic.jpg"
const val menuEnabledKey:String = "menuEnabled"
const val imgModifiedBool:String = "imgModifiedBool"
const val imgPath:String = "imgPath"
const val tempImgKey:String = "tempImg"
const val imgRotateDegreeKey:String = "imgRotateDegree"
val emailRegex:Regex= Regex("^([a-z0-9]([-a-z0-9_+.][a-z0-9])*)+@([a-z0-9-_]+).((arpa|root|aero|biz|cat|com|coop|edu|gov|info|int|jobs|mil|mobi|museum|name|net|org|pro|tel|travel|ac|ad|ae|af|ag|ai|al|am|an|ao|aq|ar|as|at|au|aw|ax|az|ba|bb|bd|be|bf|bg|bh|bi|bj|bm|bn|bo|br|bs|bt|bv|bw|by|bz|ca|cc|cd|cf|cg|ch|ci|ck|cl|cm|cn|co|cr|cu|cv|cx|cy|cz|de|dj|dk|dm|do|dz|ec|ee|eg|er|es|et|eu|fi|fj|fk|fm|fo|fr|ga|gb|gd|ge|gf|gg|gh|gi|gl|gm|gn|gp|gq|gr|gs|gt|gu|gw|gy|hk|hm|hn|hr|ht|hu|id|ie|il|im|in|io|iq|ir|is|it|je|jm|jo|jp|ke|kg|kh|ki|km|kn|kr|kw|ky|kz|la|lb|lc|li|lk|lr|ls|lt|lu|lv|ly|ma|mc|md|mg|mh|mk|ml|mm|mn|mo|mp|mq|mr|ms|mt|mu|mv|mw|mx|my|mz|na|nc|ne|nf|ng|ni|nl|no|np|nr|nu|nz|om|pa|pe|pf|pg|ph|pk|pl|pm|pn|pr|ps|pt|pw|py|qa|re|ro|ru|rw|sa|sb|sc|sd|se|sg|sh|si|sj|sk|sl|sm|sn|so|sr|st|su|sv|sy|sz|tc|td|tf|tg|th|tj|tk|tl|tm|tn|to|tp|tr|tt|tv|tw|tz|ua|ug|uk|um|us|uy|uz|va|vc|ve|vg|vi|vn|vu|wf|ws|ye|yt|yu|za|zm|zw)|([a-z]+(.[a-z]+)*))$",RegexOption.IGNORE_CASE)
val textRegex:Regex= "[^A-Za-z\\s-']".toRegex()
val priceRegex:Regex = "[0-9]{1,6}[\\.,]?[0-9]{0,2}".toRegex()
const val singleItemKey: String = "singleItemKey"
const val itemsKey: String = "itemsKey"
const val itemsOnSaleKey: String = "itemsOnSaleKey"
const val itemPicNamePrefix: String = "item_pic_"
const val itemPicNameExtension: String = ".jpg"
const val itemFolderName: String = "items"
const val singleUserUidKey: String = "profileUidKey"
const val singleUserKey: String = "singleUserKey"
const val transactionIdKey: String = "transactionIdKey"
const val newFlagKey: String = "newFlagKey"

fun Fragment.hideKeyboard() {view?.let { activity?.hideKeyboard(it) }}
fun Activity.hideKeyboard() {hideKeyboard(currentFocus ?: View(this))}

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun getCurrentLocale(context: Context) : Locale
{
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        context.resources.configuration.locales[0]
    } else {    // deprecated only if needed
        context.resources.configuration.locale
    }
}




/*    return mapOf(
        "Arts & Crafts"         to context.resources.getStringArray(R.array.art_array),
        "Sports & Hobby"        to arrayOf( "Sports and Outdoors", "Outdoor Recreation", "Sports & Fitness", "Pet Supplies"),
        "Baby"                  to arrayOf( "Apparel & Accessories", "Baby & Toddler Toys", "Car Seats & Accessories", "Pregnancy & Maternity", "Strollers & Accessories"),
        "Women's fashion"       to arrayOf( "Clothing", "Shoes", "Watches", "Handbags", "Accessories"),
        "Men's fashion"         to arrayOf( "Clothing", "Shoes", "Watches", "Accessories"),
        "Electronics"           to arrayOf( "Computers", "Monitors", "Printers & Scanners", "Camera & Photo", "Smartphone & Tablet", "Audio", "Television & Video",
                                            "Video Game Consoles", "Wearable Technology", "Accessories & Supplies", "Irons & Steamers", "Vacuums & Floor Care"),
        "Games & Videogames"    to arrayOf( "Action Figures & Statues", "Arts & Crafts", "Building Toys", "Dolls & Accessories", "Kids' Electronics",
                                            "Learning & Education", "Tricycles, Scooters & Wagons", "Videogames"),
        "Automotive"            to arrayOf( "Car Electronics & Accessories", "Accessories", "Motorcycle & Powersports", "Replacement Parts", "RV Parts & Accessories", "Tools & Equipment")
)*/
lateinit var  categoryMap:Map<String, Array<String>>
lateinit var statusArray: Array<String>


fun getResizedBitmap(image: Bitmap): Bitmap? {
    var maxSize = 600 // editare if needed, max dimension
    var width = image.width
    var height = image.height
    val bitmapRatio = width.toFloat() / height.toFloat()
    if (bitmapRatio > 0) {
        width = maxSize
        height = (width / bitmapRatio).toInt()
    } else {
        height = maxSize
        width = (height * bitmapRatio).toInt()
    }
    return Bitmap.createScaledBitmap(image, width, height, true)
}

fun getAddressFromCoordinates(latitude: Double, longitude: Double, context: Context) : String
{
    if(latitude > 90.0 || longitude > 180)
        return ""

    val addresses = Geocoder(context).getFromLocation(latitude, longitude, 1)
    return addresses[0].getAddressLine(0)
}

fun setLocationMarker(location: Pair<Double, Double>, googleMap: GoogleMap, textLocation: TextInputEditText, context: Context) : Marker
{
    val latLng = LatLng(location.first, location.second)
    val marker = googleMap.addMarker(MarkerOptions().position(latLng))
    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f), 500, null)
    setLocationText(latLng.latitude, latLng.longitude, textLocation, context)
    return marker
}

fun setLocationText(latitude: Double, longitude: Double, textLocation: TextInputEditText, context: Context)
{
    textLocation.setText(getAddressFromCoordinates(latitude, longitude, context))
}

fun getLocationFromDocument(document: DocumentSnapshot) : Pair<Double, Double>
{
    @Suppress("UNCHECKED_CAST") val locationHashMap: HashMap<String, String>? = (document.get("location") as HashMap<String, String>?)
    var location = 91.0 to 181.0
    if(locationHashMap != null) {
        val latitude = locationHashMap["first"]?.toDouble() ?: 91.0
        val longitude = locationHashMap["second"]?.toDouble() ?: 181.0
        location = latitude to longitude
    }

    return location
}

fun getUserFromDocument(snapshot: DocumentSnapshot): User {
    return  User(snapshot.getString("fullName") ?: "",snapshot.getString("nickname") ?: "",
        phoneNumber = snapshot.getString("phoneNumber") ?: "",location = getLocationFromDocument(snapshot),
        totRate = snapshot.getDouble("totRate")?:0.0, rateCount = snapshot.getLong("rateCount")?:-1)
}

fun setHiddenSwitchStatus(statusInt: Int, switch: SwitchMaterial, wrapperLayout: LinearLayout)
{
    when(statusInt)
    {
        0 -> switch.isChecked = false
        2 -> switch.isChecked = true
        else -> wrapperLayout.visibility = View.GONE
    }
}

var heightMap : Int = 0


fun expand(v:View) {
    val matchParentMeasureSpec = View.MeasureSpec.makeMeasureSpec((v.parent as View).width, View.MeasureSpec.EXACTLY)
    val wrapContentMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    v.measure(matchParentMeasureSpec, wrapContentMeasureSpec)
    val targetHeight = v.measuredHeight
    v.layoutParams.height = 1
    v.visibility = View.VISIBLE
    val a = object: Animation() {
        override fun applyTransformation(interpolatedTime:Float, t: Transformation) {
            v.layoutParams.height = (if (interpolatedTime == 1f)
                heightMap
            else
                (targetHeight * interpolatedTime).toInt())
            v.requestLayout()
        }
        override fun willChangeBounds():Boolean {
            return true
        }
    }
    a.duration = 300
    v.startAnimation(a)
}

fun collapse(v:View) {
    val a = object: Animation() {
        override fun applyTransformation(interpolatedTime:Float, t: Transformation) {
            if (interpolatedTime == 1f)
            {
                v.visibility = View.GONE
            }
            else
            {
                v.layoutParams.height = 0

                v.requestLayout()
            }
        }
        override fun willChangeBounds():Boolean {
            return true
        }
    }
    a.duration = 300
    v.startAnimation(a)
}