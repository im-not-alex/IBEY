package it.polito.mad.group33.ibey.viewmodel

import android.os.Bundle
import androidx.lifecycle.ViewModel

abstract class ParcelableViewModel : ViewModel() {
    abstract fun writeTo(bundle: Bundle)
    abstract fun readFrom(bundle: Bundle)
}
