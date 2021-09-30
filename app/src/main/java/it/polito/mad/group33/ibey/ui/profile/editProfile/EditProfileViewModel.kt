package it.polito.mad.group33.ibey.ui.profile.editProfile

import android.graphics.Bitmap
import android.os.Bundle
import it.polito.mad.group33.ibey.*
import it.polito.mad.group33.ibey.User
import it.polito.mad.group33.ibey.repository.firebaseRepository
import it.polito.mad.group33.ibey.viewmodel.ParcelableViewModel

class EditProfileViewModel : ParcelableViewModel() {
    lateinit var newUser: User
    var dialogDismiss: Boolean = false

    override fun writeTo(bundle: Bundle) {
        bundle.putSerializable(new_profileKey, newUser)
    }

    override fun readFrom(bundle: Bundle) {
        newUser=bundle.getSerializable(new_profileKey) as User
    }

    fun saveUserStorage(image: Bitmap) = firebaseRepository.uploadImage(image, "Users/" + newUser.uid, "profile_0.jpg")
}
