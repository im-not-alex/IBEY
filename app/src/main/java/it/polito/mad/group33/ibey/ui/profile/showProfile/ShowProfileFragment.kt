package it.polito.mad.group33.ibey.ui.profile.showProfile

import android.content.res.Configuration
import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import it.polito.mad.group33.ibey.MainActivity
import it.polito.mad.group33.ibey.R
import it.polito.mad.group33.ibey.User
import it.polito.mad.group33.ibey.singleUserKey
import kotlinx.android.synthetic.main.profile_show_rate.*
import kotlinx.android.synthetic.main.show_profile_fragment.*


abstract class ShowProfileFragment : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            val params = toolbar_wrapper.layoutParams
            params.height = (activity as MainActivity).availableSpace()/3
            toolbar_wrapper.layoutParams = params
        }
    }
    abstract fun getUserImage(name:String, location:String)
    abstract fun setUserInformation(u: User)
    fun setUserRate(u: User) {
        if(u.rateCount!=-1L){
            profile_show_rate.visibility=View.VISIBLE
            val rating:Float=(u.totRate/(u.rateCount+1)).toFloat()
            rating_bar_review.rating = rating
            if(u.rateCount==1L)
                text_review.text= requireContext().resources.getString(R.string.number_review_single,u.rateCount,rating)
            else
                text_review.text= requireContext().resources.getString(R.string.number_review,u.rateCount,rating)

        }else
            profile_show_rate.visibility=View.GONE

        if(u.rateCount>0){
            profile_show_rate.setOnClickListener {
                val bundle = bundleOf(singleUserKey to u.toString())
                NavHostFragment.findNavController(this).navigate(getReviewNavigation(), bundle)
            }
            val formattedText = SpannableString(text_review.text)
            formattedText.setSpan(UnderlineSpan(), text_review.text.indexOf("(")+1, text_review.text.indexOf(")"), 0)
            text_review.text = formattedText
        }
    }

    abstract fun getReviewNavigation():Int

}
