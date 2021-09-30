package it.polito.mad.group33.ibey.viewmodel

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class BundleAwareViewModelFactory(
        private val bundle: Bundle?,
        private val provider: ViewModelProvider.Factory) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val viewModel = provider.create(modelClass)
        if (viewModel is ParcelableViewModel) {
            bundle?.let { viewModel.readFrom(it) }
        }
        return viewModel
    }
}
