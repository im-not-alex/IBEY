package it.polito.mad.group33.ibey

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.Window
import androidx.fragment.app.FragmentActivity
import it.polito.mad.group33.ibey.repository.firebaseRepository
import it.polito.mad.group33.ibey.ui.item.itemDetail.BoughtViewModel
import it.polito.mad.group33.ibey.ui.item.itemDetail.ItemDetailsViewModel
import kotlinx.android.synthetic.main.rate_popup.*

// TODO bisognerà passargli il view Model
class RateDialog(activity: FragmentActivity,val boughtViewModel: BoughtViewModel,val itemDetailsViewModel: ItemDetailsViewModel): Dialog(activity), View.OnClickListener {
    var cancel = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.rate_popup)
        popup_rate_send_button.setOnClickListener(this)
        popup_rate_cancel_button.setOnClickListener(this)

        //boughtViewModel.dialogOpen=true
        // set initial value
        rating_bar.rating= boughtViewModel.actualRate.value!!
        setDescription(boughtViewModel.actualRate.value!!)
        rating_bar.setOnRatingBarChangeListener { _, rating, _ ->
            setRating(rating)
        }

        this.setOnDismissListener {
            // if not OK clear the initial stars
            if(!boughtViewModel.reviewDone.value!!) boughtViewModel.actualRate.value = 0.0F
            //boughtViewModel.dialogOpen=false
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.popup_rate_send_button -> {
                if(boughtViewModel.actualRate.value == 0.0F){
                    textViewResult.text = context.resources.getString(R.string.please_rate)
                }else{
                    boughtViewModel.comment.value=text_rate_description.text.toString().trim()
                    boughtViewModel.reviewDone.value=true
                    // TODO posso passare itemDetailsViewModel.myItem
                    firebaseRepository.addReview(boughtViewModel.actualRate.value!!,boughtViewModel.comment.value?:"",boughtViewModel.transactionId)
                    dismiss()
                }
            }
            R.id.popup_rate_cancel_button -> {
                cancel = true
                cancel()
            }
            else -> cancel()
        }
    }

    private fun setRating(rating: Float) {
        boughtViewModel.actualRate.value = rating
        setDescription(rating)
    }

    private fun setDescription(rating: Float) {
        if((rating.toInt()-1)>=0)
            textViewResult.text = context.resources.getStringArray(R.array.rate)[rating.toInt()-1]
        else
            textViewResult.text = ""
    }


}
