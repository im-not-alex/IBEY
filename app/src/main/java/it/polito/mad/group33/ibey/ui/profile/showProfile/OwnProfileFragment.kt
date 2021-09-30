package it.polito.mad.group33.ibey.ui.profile.showProfile

import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.MapView
import com.google.android.libraries.maps.model.Marker
import it.polito.mad.group33.ibey.*
import it.polito.mad.group33.ibey.ui.profile.UserViewModel
import kotlinx.android.synthetic.main.profile_show_fields.*
import kotlinx.android.synthetic.main.show_profile_fragment.*
import kotlinx.android.synthetic.main.toolbar_show_pic.*

class OwnProfileFragment : ShowProfileFragment() {
    private lateinit var userViewModel: UserViewModel
    private lateinit var mapView : MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var locationMarker: Marker
    private var mapShown: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)
        val view = inflater.inflate(R.layout.show_profile_fragment,container,false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).populateToolbar(view.findViewById(R.id.toolbar))
        id_edit.setOnClickListener{ findNavController().navigate(R.id.action_showProfileFragment_to_editProfileFragment)}
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if(userViewModel.img.value==null)
            profile_pic.setImageResource(R.drawable.ic_avatar)
        else
            profile_pic.setImageBitmap(userViewModel.img.value)
        userViewModel.user.observe(viewLifecycleOwner, Observer {
            setUserInformation(it)
            setUserRate(it)
            manageMapView(savedInstanceState, it)
        })
    }

    override fun onPause() {
        if(this::mapView.isInitialized)
            mapView.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if(this::mapView.isInitialized)
            mapView.onResume()
    }

    override fun onDestroy() {
        if(this::mapView.isInitialized)
            mapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        if(this::mapView.isInitialized)
            mapView.onLowMemory()
        super.onLowMemory()
    }

    override fun setUserInformation(u: User)
    {
        id_fullName!!.setText(u.fullName)
        id_nickname!!.setText(u.nickname)
        if(u.phoneNumber!="")
            id_phoneNumber!!.setText(u.phoneNumber)
        else
            text_phoneNumber.visibility=View.GONE

        id_email!!.setText(u.email)
    }


    override fun getUserImage(name:String, location:String){}

    override fun getReviewNavigation() = R.id.action_OwnProfileFragment_to_showReviews

    private fun manageMapView(savedInstanceState: Bundle?, user: User)
    {
        requireActivity().findViewById<MapView>(R.id.idShowProfileLocationMap).visibility = View.GONE
        if(user.location.first <= 90.0 && user.location.second <= 180.0) {
            setLocationText(user.location.first, user.location.second, textProfileLocation, requireContext())
            textProfileLocation.setOnClickListener { onClickListenerShowUnShowMap(savedInstanceState, user) }
            idProfileLocation.setOnClickListener { onClickListenerShowUnShowMap(savedInstanceState, user) }
        } else {
            idProfileLocation.visibility = View.GONE
        }
    }

    private fun onClickListenerShowUnShowMap(savedInstanceState: Bundle?, user: User)
    {
        if (!mapShown) {
            if(!this::mapView.isInitialized) {
                mapView = requireActivity().findViewById(R.id.idShowProfileLocationMap)
                mapView.onCreate(savedInstanceState)
            }
            else
                mapView.onResume()


            mapView.getMapAsync { googleMap ->
                this.googleMap = googleMap
                locationMarker = setLocationMarker(user.location, googleMap, textProfileLocation, requireContext())
                idProfileLocation.helperText = getString(R.string.hideMap)

                val endIcon = resources.getDrawable(R.drawable.ic_keyboard_arrow_up, null)
                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    endIcon.setTint(resources.getColor(R.color.primaryDarkColor, null))
                else
                    endIcon.setTint(resources.getColor(R.color.primaryDarkColor))
                idProfileLocation.endIconDrawable = endIcon
            }
            expand(mapView)
        } else {
            idProfileLocation.helperText = getString(R.string.showInMap)

            val endIcon = resources.getDrawable(R.drawable.ic_keyboard_arrow_down, null)
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                endIcon.setTint(resources.getColor(R.color.primaryDarkColor, null))
            else
                endIcon.setTint(resources.getColor(R.color.primaryDarkColor))
            idProfileLocation.endIconDrawable = endIcon
            collapse(mapView)
        }
        mapShown = !mapShown
    }


}