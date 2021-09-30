package it.polito.mad.group33.ibey.ui.edits

import android.app.AlertDialog
import android.graphics.drawable.Drawable
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.navigation.fragment.findNavController
import it.polito.mad.group33.ibey.R
import it.polito.mad.group33.ibey.hideKeyboard

abstract class EditFragment() : ImageHandlerFragment() {

    // creazione menu con tick conferma e annulla
    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.id_confirm).isEnabled = (viewModelImageHandler.menuEnabled || viewModelImageHandler.imgModified)
        // menu.findItem(R.id.id_confirm).isVisible = menu_enabled; // alternative version, hides the icon
        val drawable: Drawable = menu.findItem(R.id.id_confirm).icon
        if (viewModelImageHandler.menuEnabled || viewModelImageHandler.imgModified) {
            DrawableCompat.setTint(
                drawable,
                ContextCompat.getColor(requireContext(),
                    R.color.tick_with_mod
                )
            )
        } else {
            DrawableCompat.setTint(
                drawable,
                ContextCompat.getColor(requireContext(),
                    R.color.tick_without_mod
                )
            )
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.editprofilebar, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.id_info -> {
                AlertDialog.Builder(requireContext())
                    .setTitle(resources.getString(R.string.info))
                    .setMessage(resources.getString(R.string.infoMessage))
                    .setNegativeButton(android.R.string.yes, null)
                    .show()

            }
            R.id.id_confirm -> {
                confirmEdits()
            }
        }
        hideKeyboard()
        return super.onOptionsItemSelected(item)
    }

    fun onBackPressedAction()
    {
        viewModelImageHandler.tempimg?.delete()
        Toast.makeText(requireContext(),getString(R.string.toastCancelledOp), Toast.LENGTH_SHORT).show()
        hideKeyboard()
    }

    abstract fun confirmEdits()

}