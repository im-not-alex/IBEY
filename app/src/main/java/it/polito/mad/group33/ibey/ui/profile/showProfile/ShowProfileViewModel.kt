package it.polito.mad.group33.ibey.ui.profile.showProfile

import android.graphics.Bitmap
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import it.polito.mad.group33.ibey.User
import it.polito.mad.group33.ibey.viewmodel.ParcelableViewModel

class ShowProfileViewModel : ParcelableViewModel() {
    var user = MutableLiveData<User>()
    var img = MutableLiveData<Bitmap?>()
    var owner:Boolean = false

    override fun writeTo(bundle: Bundle) {}

    override fun readFrom(bundle: Bundle) {
    }
}