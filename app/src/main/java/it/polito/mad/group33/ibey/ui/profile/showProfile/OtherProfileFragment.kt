package it.polito.mad.group33.ibey.ui.profile.showProfile

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.MapView
import com.google.android.libraries.maps.model.Marker
import it.polito.mad.group33.ibey.*
import it.polito.mad.group33.ibey.repository.firebaseRepository
import it.polito.mad.group33.ibey.viewmodel.BundleAwareViewModelFactory
import kotlinx.android.synthetic.main.profile_public_show_fields.*
import kotlinx.android.synthetic.main.toolbar_show_pic.*


class OtherProfileFragment : ShowProfileFragment() {
    private lateinit var viewModel: ShowProfileViewModel
    private lateinit var mapView : MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var locationMarker: Marker
    private var mapShown: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        viewModel = ViewModelProvider(this,
            BundleAwareViewModelFactory(savedInstanceState, ViewModelProvider.NewInstanceFactory())
        ).get(ShowProfileViewModel::class.java)
        val bundle = this.arguments
        if (bundle != null) {
            viewModel.user.value = User(bundle.getString(singleUserKey)!!)
        }
        return inflater.inflate(R.layout.show_profile_public_fragment,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).populateToolbar(view.findViewById(R.id.toolbar))
        (activity as MainActivity).lockSwipeDrawer()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        getUserImage("profile_0.jpg","Users/"+viewModel.user.value!!.uid)
        viewModel.img.observe(viewLifecycleOwner, Observer { profile_pic.setImageBitmap(it) })
        viewModel.user.observe(viewLifecycleOwner, Observer {
            setUserInformation(it)
            setUserRate(it)
            manageMapView(savedInstanceState, it)
        })
    }

    override fun setUserInformation(u: User) {
        id_fullName!!.setText(u.fullName)
        id_nickname!!.setText(u.nickname)
        if (u.phoneNumber != "") {
            id_phoneNumber!!.setText(u.phoneNumber)
            id_phoneNumber.setOnClickListener {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:"+u.phoneNumber)
                startActivity(intent)
            }
            id_phoneNumber.paintFlags = id_phoneNumber.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            text_phoneNumber.helperText = getString(R.string.press_to_call)
        }
        else
            text_phoneNumber.visibility = View.GONE
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

    override fun getUserImage(name:String, location:String){
        firebaseRepository.getImg(name, location).addOnSuccessListener {
            Thread(Runnable {
                Glide.with(requireContext()).asBitmap().load(it).error(R.mipmap.ic_launcher).into(object : CustomTarget<Bitmap>(){
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        // Save image on viewmodel.livedata
                        viewModel.img.postValue(resource)
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {}})}).start()
        }.addOnFailureListener {
            Log.d("profile_pic",it.message)
        }
    }

    override fun getReviewNavigation() = R.id.action_otherProfileFragment_to_showReviews

    private fun manageMapView(savedInstanceState: Bundle?, user: User)
    {
        requireActivity().findViewById<MapView>(R.id.idShowPublicProfileLocationMap).visibility = View.GONE
        if(user.location.first <= 90.0 && user.location.second <= 180.0) {
            setLocationText(user.location.first, user.location.second, textPublicProfileLocation, requireContext())
            textPublicProfileLocation.setOnClickListener { onClickListenerShowUnShowMap(savedInstanceState, user) }
            idPublicProfileLocation.setOnClickListener { onClickListenerShowUnShowMap(savedInstanceState, user) }
        } else {
            idPublicProfileLocation.visibility = View.GONE
        }
    }

    private fun onClickListenerShowUnShowMap(savedInstanceState: Bundle?, user: User)
    {
        if (!mapShown) {
            if(!this::mapView.isInitialized) {
                mapView = requireActivity().findViewById(R.id.idShowPublicProfileLocationMap)
                mapView.onCreate(savedInstanceState)
            }
            else
                mapView.onResume()


            mapView.getMapAsync { googleMap ->
                this.googleMap = googleMap
                locationMarker = setLocationMarker(user.location, googleMap, textPublicProfileLocation, requireContext())
                idPublicProfileLocation.helperText = getString(R.string.hideMap)

                val endIcon = resources.getDrawable(R.drawable.ic_keyboard_arrow_up, null)
                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    endIcon.setTint(resources.getColor(R.color.primaryDarkColor, null))
                else
                    endIcon.setTint(resources.getColor(R.color.primaryDarkColor))
                idPublicProfileLocation.endIconDrawable = endIcon
            }

            expand(mapView)
        } else {
            idPublicProfileLocation.helperText = getString(R.string.showInMap)

            val endIcon = resources.getDrawable(R.drawable.ic_keyboard_arrow_down, null)
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                endIcon.setTint(resources.getColor(R.color.primaryDarkColor, null))
            else
                endIcon.setTint(resources.getColor(R.color.primaryDarkColor))
            idPublicProfileLocation.endIconDrawable = endIcon

        collapse(mapView)
        }
        mapShown = !mapShown
    }


}