package it.polito.mad.group33.ibey

import android.app.Dialog
import android.os.Bundle
import android.view.Window
import androidx.fragment.app.FragmentActivity

class LoadingDialog(activity: FragmentActivity,theme:Int): Dialog(activity,theme) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.loading_popup)
    }
}
