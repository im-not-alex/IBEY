package it.polito.mad.group33.ibey.ui.item.itemDetail

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.model.*
import it.polito.mad.group33.ibey.*
import it.polito.mad.group33.ibey.repository.firebaseRepository
import it.polito.mad.group33.ibey.ui.profile.UserViewModel
import kotlinx.android.synthetic.main.item_details_fragment.*
import kotlinx.android.synthetic.main.item_show_fields.*
import kotlinx.android.synthetic.main.item_show_seller.*
import java.text.DecimalFormat


open class OtherItemDetailsFragment : ItemDetailsFragment() {
    protected lateinit var userViewModel: UserViewModel
    private val LOCATION_PERMISSION_ID = 42
    private var currentLocation: Marker? = null
    private var polyline: Polyline? = null

    lateinit var mainHandler: Handler
    private val likeUpdater =
        Runnable {
            viewModel.clickable=0
            viewModel.myItem?.id?.let { firebaseRepository.itemLikeDislike(it,viewModel.interest!!) }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)
        viewModel = ViewModelProvider(this).get(ItemDetailsViewModel::class.java)
        val bundle = this.arguments
        viewModel.myItem= Item(bundle?.getString(singleItemKey)!!)
        if(viewModel.interest==null)
            viewModel.interest = userViewModel.interests.value?.contains(viewModel.myItem?.id) ?: false

        polyline = null
        currentLocation = null
        return inflater.inflate(R.layout.item_details_public_fragment, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        item_show_seller_button.text = getString(R.string.soldBy)
        // richiesta al db dell'user da visualizzare
        viewModel.retrieveSeller(viewModel.myItem?.seller!!.uid)
        viewModel.sellerAquired.observe(viewLifecycleOwner, Observer {
            if(it==true){ // user profile received
                // aggiorno bottone (eg mettere stelle valutazione user?
                item_show_seller_button.text = getString(R.string.soldBy) + " " + viewModel.myItem?.seller!!.nickname

                // abilitare il click con passaggio bundle
                val bundle = bundleOf(singleUserKey to viewModel.myItem?.seller!!.toString())
                item_show_seller_button.setOnClickListener {
                    findNavController().navigate(getNavAction(), bundle)
                }
            }
        })
        // configurazione logica invio interest
        mainHandler = Handler(Looper.getMainLooper())
        id_edit.setOnClickListener {
            viewModel.interest=!viewModel.interest!!  // inverto l'interesse
            if(viewModel.interest!!){id_edit.setImageResource(R.drawable.star_icon)}else{id_edit.setImageResource(R.drawable.not_star_icon)}
            if(viewModel.clickable==0)
                mainHandler.postDelayed(likeUpdater,10*1000)
            viewModel.clickable++;
            Log.d("star","Star button clicked ${viewModel.clickable} times")
        }
    }

    open fun getNavAction(): Int {
        return R.id.action_otherItemDetailsFragment_to_otherProfileFragment
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_ID) {
            if((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                checkPermissionAndEventuallyGetCurrentLocation()
            }
        }
    }

    override fun onPause() {
        viewModel.myItem?.id?.let { firebaseRepository.itemLikeDislike(it,viewModel.interest!!) }
        mainHandler.removeCallbacks(likeUpdater)
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if(viewModel.interest!!){id_edit.setImageResource(R.drawable.star_icon)}else{id_edit.setImageResource(R.drawable.not_star_icon)}
    }

    override fun setItemInformation(myItem: Item)
    {
        text_item_title!!.setText(myItem.title)
        if(myItem.description!="")
            text_item_description!!.setText(myItem.description)
        else
            id_item_description.visibility=View.GONE
        if (myItem.price!=-1.0){ // default case, leave the box empty
            val decimalFormat = DecimalFormat.getCurrencyInstance(getCurrentLocale(this.requireContext()))
            text_item_price!!.setText(decimalFormat.format(myItem.price))
        }
        text_item_category!!.setText(myItem.category)
        text_item_date!!.setText(myItem.expiryDateString)
        statusHiddenLayout.visibility = View.GONE
        getItemImage("item_0.jpg","Users/"+myItem.seller!!.uid + "/" + myItem.id)   // seller uid corrisponde generico: tanto per proprio user quanto per altro seller
    }

    override fun extraMapViewManagementMethods()
    {
        if(!checkPermissionAndEventuallyGetCurrentLocation()) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_ID
            )
        }
    }

    private fun checkPermissionAndEventuallyGetCurrentLocation() : Boolean
    {
        if(checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val locationProvider =
                LocationServices.getFusedLocationProviderClient(requireActivity())
            locationProvider.lastLocation.addOnSuccessListener { result ->
                 if(result != null) setCurrentLocationOnMap(result)
            }.addOnFailureListener {
                Log.d("lastLocation","Error on lastLocation" + it.message )
            }

            val locationRequest = LocationRequest()
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            locationRequest.interval = 0
            locationRequest.fastestInterval = 0
            locationRequest.numUpdates = 1

            locationProvider.requestLocationUpdates(locationRequest, object : LocationCallback() {
                override fun onLocationResult(p0: LocationResult?) {
                    if(p0 != null)
                        setCurrentLocationOnMap(p0.lastLocation)
                }
            }, Looper.myLooper())
            return true
        }
        return false
    }

    private fun setCurrentLocationOnMap(location: Location)
    {
        if(!isGoogleMapInitialized() || !isLocationMarkerInitialized())
            return

        if(currentLocation == null)
            currentLocation = googleMap.addMarker(MarkerOptions().position(LatLng(location.latitude, location.longitude))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)))
        else
            currentLocation!!.position = LatLng(location.latitude, location.longitude)

        googleMap.setOnMarkerClickListener { marker -> marker != null && marker == currentLocation }

        val builder = LatLngBounds.builder()
        builder.include(currentLocation!!.position)
        builder.include(locationMarker.position)
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150), 500, null)

        if(googleMap.cameraPosition.zoom > 15)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(getCenter(currentLocation!!.position, locationMarker.position), 15f))

        if(polyline == null)
            polyline = googleMap.addPolyline(PolylineOptions().add(currentLocation!!.position, locationMarker.position)
                .width(5f).color(Color.BLACK))
        else {
            polyline!!.points = listOf(currentLocation!!.position, locationMarker.position)
            polyline!!.width = 5f
            polyline!!.color = Color.BLACK
        }
    }

    private fun getCenter(first: LatLng, second: LatLng): LatLng = LatLng((first.latitude + second.latitude) / 2.0, (first.longitude + second.longitude) / 2.0)
}