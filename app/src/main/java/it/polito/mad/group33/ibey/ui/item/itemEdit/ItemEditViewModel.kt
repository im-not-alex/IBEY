package it.polito.mad.group33.ibey.ui.item.itemEdit

import android.graphics.Bitmap
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import it.polito.mad.group33.ibey.Item
import it.polito.mad.group33.ibey.*
import it.polito.mad.group33.ibey.repository.firebaseRepository
import it.polito.mad.group33.ibey.viewmodel.ParcelableViewModel

class ItemEditViewModel : ParcelableViewModel() {
    lateinit var myItem: Item // item received, to be modified
    lateinit var newItem: Item
    var dialogDismiss: Boolean = false

    var imgdone = MutableLiveData<Boolean>(false)
    var done = MutableLiveData<Boolean>(false)

    override fun writeTo(bundle: Bundle) {
        bundle.putSerializable(itemsKey, newItem)

    }

    override fun readFrom(bundle: Bundle) {
        newItem = Item(bundle.getString(itemsKey)!!)
    }

    fun saveItemToFirebase() = firebaseRepository.saveItem(newItem)

    fun saveImagetoStorage(image:Bitmap) = firebaseRepository.uploadImage(image, "Users/" + newItem.seller!!.uid + "/" + newItem.id, "item_0.jpg")
}

//    fun saveItemToFirebase(image:Bitmap?):UploadTask? {
//        firebaseRepository.saveItem(newItem).add { task ->
//            {
//                return if (image != null) {
//                    if (task.result != null && newItem.id == "")
//                        firebaseRepository.uploadImage(
//                            image,
//                            "Users/" + newItem.seller!!.uid + "/" + task.result!!.data,
//                            "0.jpg"
//                        )
//                    else
//                        firebaseRepository.uploadImage(
//                            image,
//                            "Users/" + newItem.seller!!.uid + "/" + newItem.id,
//                            "0.jpg"
//                        )
//                } else
//                    null
//            }
//        }
//    }
