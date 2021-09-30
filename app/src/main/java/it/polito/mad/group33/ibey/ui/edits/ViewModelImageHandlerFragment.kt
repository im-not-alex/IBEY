package it.polito.mad.group33.ibey.ui.edits

import android.graphics.Bitmap
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import it.polito.mad.group33.ibey.imgModifiedBool
import it.polito.mad.group33.ibey.imgRotateDegreeKey
import it.polito.mad.group33.ibey.menuEnabledKey
import it.polito.mad.group33.ibey.tempImgKey
import it.polito.mad.group33.ibey.viewmodel.ParcelableViewModel
import java.io.File

open class ViewModelImageHandlerFragment : ParcelableViewModel() {

    var menuEnabled: Boolean = false
    var imgModified: Boolean = false
    var tempimg: File? = null
    var imgRotateDegree: Int = 0
    var img = MutableLiveData<Bitmap?>()
    var imgdone = MutableLiveData<Boolean>(false)

    override fun writeTo(bundle: Bundle) {
        bundle.putBoolean(menuEnabledKey, menuEnabled)
        bundle.putBoolean(imgModifiedBool, imgModified)
        bundle.putSerializable(tempImgKey, tempimg)
        bundle.putInt(imgRotateDegreeKey, imgRotateDegree)

    }

    override fun readFrom(bundle: Bundle) {
        menuEnabled = bundle.getBoolean(menuEnabledKey)
        imgModified = bundle.getBoolean(imgModifiedBool)
        tempimg = bundle.getSerializable(tempImgKey) as File?
        imgRotateDegree = bundle.getInt(imgRotateDegreeKey)
    }
}