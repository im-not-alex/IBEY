package it.polito.mad.group33.ibey.ui.profile.editProfile

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.MapView
import com.google.android.libraries.maps.model.*
import it.polito.mad.group33.ibey.*
import it.polito.mad.group33.ibey.ui.edits.EditFragment
import it.polito.mad.group33.ibey.ui.profile.UserViewModel
import kotlinx.android.synthetic.main.edit_profile_fragment.*
import kotlinx.android.synthetic.main.profile_edit_fields.*
import kotlinx.android.synthetic.main.toolbar_show_pic.*
import kotlin.properties.Delegates


class EditProfileFragment : EditFragment() {
    private lateinit var viewModel: EditProfileViewModel
    private lateinit var edittexts:List<View>
    private lateinit var userViewModel:UserViewModel
    private val LOCATION_PERMISSION_ID = 42
    lateinit var mapView : MapView
    lateinit var googleMap: GoogleMap
    var activeMarker: Marker? = null
    var firstLogin by Delegates.notNull<Boolean>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        viewModel = ViewModelProvider(this).get(EditProfileViewModel::class.java)
        val view = inflater.inflate(R.layout.edit_profile_fragment, container, false)
        manageMapView(view, savedInstanceState)
        return view
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firstLogin=this.arguments?.getBoolean("firstLogin")?:false
        if(!firstLogin) {
            (activity as MainActivity).populateToolbar(view.findViewById(R.id.toolbar))
            (activity as MainActivity).lockSwipeDrawer()
        } else
            (activity as MainActivity).setSupportActionBar(view.findViewById(R.id.toolbar))

        id_takepic.setOnClickListener {
            showPopup(it)
        }
        // resize toolbar
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            val params = toolbar_wrapper.layoutParams
            params.height = (activity as MainActivity).availableSpace()/3
            toolbar_wrapper.layoutParams = params
        }

        edittexts= listOf(id_fullName,id_email,id_nickname,id_location)
        for(elem in edittexts)
            elem.setOnFocusChangeListener { v, hasFocus ->
                if(hasFocus) checkCompiledForms(v,clear = true)
                else checkCompiledForms(v)}
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


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)

        dialogBuild()

        if (savedInstanceState == null) {
            viewModel.newUser = userViewModel.user.value?.copy() ?: User()
            if (userViewModel.img.value != null)
                viewModelImageHandler.img.value = userViewModel.img.value
        }
        if (viewModelImageHandler.tempimg != null && viewModelImageHandler.imgModified)
            viewModelImageHandler.img.value = BitmapFactory.decodeFile(viewModelImageHandler.tempimg?.absolutePath)
        else if(userViewModel.img.value!=null)
            viewModelImageHandler.img.value=userViewModel.img.value
        if (viewModelImageHandler.imgModified) {
            viewModelImageHandler.img.value = viewModelImageHandler.img.value?.let { rotateImage(it, viewModelImageHandler.imgRotateDegree) }
        }

        setEditable(userViewModel.user.value!!)
        if(userViewModel.img.value==null)
            profile_pic.setImageResource(R.drawable.ic_avatar)

    }

    private fun retrieveInsertedUser(): User {
        // Take all the inputs from the user

        return User(
            id_fullName.text.toString().trim(),
            id_nickname.text.toString().trim(),
            id_email.text.toString().trim(),
            id_phoneNumber.text.toString().trim(),
            viewModel.newUser.location)
            .apply{ this.uid = userViewModel.user.value?.uid ?: "" }
    }

    private fun setEditable(u: User) {

        val inputUserTextWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val newUserInside = retrieveInsertedUser()
                val tmpBool: Boolean = viewModelImageHandler.menuEnabled
                viewModelImageHandler.menuEnabled = ((newUserInside != userViewModel.user.value) || viewModelImageHandler.imgModified)
                if (tmpBool != viewModelImageHandler.menuEnabled) {
                    requireActivity().invalidateOptionsMenu()
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        }

        id_fullName.setText(u.fullName)
        id_nickname.setText(u.nickname)
        id_email.setText(u.email)
        id_phoneNumber.setText(u.phoneNumber)
        id_fullName.addTextChangedListener(inputUserTextWatcher)
        id_nickname.addTextChangedListener(inputUserTextWatcher)
        id_email.addTextChangedListener(inputUserTextWatcher)
        id_phoneNumber.addTextChangedListener(inputUserTextWatcher)
        id_location.addTextChangedListener(inputUserTextWatcher)
        
        id_fullName.filters = arrayOf(filter)

        id_email.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus && (v as EditText).text != null) {
                if (!v.text.toString().matches(emailRegex)) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.toastInvalidEmail),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        text_location.setEndIconOnClickListener {
            id_location.setText("")
            activeMarker?.isVisible = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.newUser = retrieveInsertedUser()
        //outState.putSerializable(profileKey,user)
        viewModel.writeTo(outState)
        viewModelImageHandler.writeTo(outState)
    }


    override fun confirmEdits() {
        if (checkCompiledForms(null,true)) {
            viewModel.newUser = retrieveInsertedUser()
            val loadingDialog = LoadingDialog(requireActivity(),R.style.LoadingDialog)
            viewModel.dialogDismiss = false
            loadingDialog.setOnDismissListener { dialog ->
                if(viewModel.dialogDismiss)
                    dialog.dismiss()
            }
            val navOptions=NavOptions.Builder().setPopUpTo(R.id.editProfileFragment,true).build()
            loadingDialog.show()
            //Toast.makeText(requireContext(),getString(R.string.toastUpdating),Toast.LENGTH_SHORT).show()
            userViewModel.saveUserToFirebase(viewModel.newUser).addOnSuccessListener {
                if(viewModelImageHandler.imgModified && viewModelImageHandler.img.value!=null){
                    viewModel.saveUserStorage(viewModelImageHandler.img.value!!)    // upload image on the server
                        .addOnFailureListener {
                            viewModel.dialogDismiss = true
                            loadingDialog.dismiss()
                        }
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(),getString(R.string.toastProfileSaved),Toast.LENGTH_SHORT).show()
                            userViewModel.img.value=viewModelImageHandler.img.value!!   // image uploaded, locally we can show the new image
                            viewModel.dialogDismiss = true
                            loadingDialog.dismiss()
                            if(firstLogin) {
                                findNavController().navigate(R.id.onSaleListFragment)
                            } else
                                findNavController().popBackStack()
                        }
                } else { // else senza update immagine
                    Toast.makeText(requireContext(),getString(R.string.toastProfileSaved),Toast.LENGTH_SHORT).show()
                    viewModel.dialogDismiss = true
                    loadingDialog.dismiss()
                    if(firstLogin)
                        findNavController().navigate(R.id.onSaleListFragment)
                    else
                        findNavController().popBackStack()                }

            }.addOnFailureListener{
                viewModel.dialogDismiss = true
                loadingDialog.dismiss()
            }
        } else {
            //menuEnabled=false
            //requireActivity().invalidateOptionsMenu()
            Toast.makeText(requireContext(),getString(R.string.toastErrorCheck),Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_ID) {
            if((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                setMyLocationEnabled()
            }
        }
    }

    private fun checkCompiledForms(view: View?, all:Boolean=false, clear :Boolean=false): Boolean {
        if(!all) {
            when (view) {
                id_fullName ->
                    if (id_fullName.text.toString().trim() == "" && !clear) {
                        text_fullName.error = getString(R.string.empty_field_error)
                        return false
                    } else text_fullName.error=null
                id_nickname ->
                    if (id_nickname.text.toString().trim() == "" && !clear) {
                        text_nickname.error = getString(R.string.empty_field_error)
                        return false
                    } else text_nickname.error=null
                id_email ->
                    if (id_email.text.toString().trim() == "" && !clear) {
                        text_email.error = getString(R.string.empty_field_error)
                        return false
                    } else if(!id_email.text.toString().trim().matches(emailRegex) && !clear){
                        text_email.error = getString(R.string.toastInvalidEmail)
                        return false
                    } else text_email.error=null
                id_location ->
                    if (id_location.text.toString().trim() == "" && !clear) {
                        text_location.error = getString(R.string.empty_field_error)
                        return false
                    } else text_location.error=null
            }
        } else {
            var res = true
            for(elem in edittexts)
                res = checkCompiledForms(elem) && res
            return res
        }
        return true
    }

    private fun manageMapView(view: View, savedInstanceState: Bundle?)
    {
        mapView = view.findViewById(R.id.idEditProfileLocationMap)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { googleMap ->
            this.googleMap = googleMap

            if(userViewModel.user.value!!.location.first <= 90.0 && userViewModel.user.value!!.location.second <= 180) {
                activeMarker = setLocationMarker(userViewModel.user.value!!.location, googleMap, id_location, requireContext())
            }

            googleMap.setOnMapClickListener { latLng ->
                if(activeMarker == null) {
                    activeMarker = googleMap.addMarker(MarkerOptions().position(latLng))
                }
                else
                    activeMarker!!.position = latLng

                activeMarker!!.isVisible = true
                id_location.setText(getAddressFromCoordinates(latLng.latitude, latLng.longitude, requireContext()))
                viewModel.newUser.location = latLng.latitude to latLng.longitude
            }

            if(!setMyLocationEnabled())
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_ID
                )

            mapView.onResume()
        }
    }

    private fun setMyLocationEnabled(): Boolean
    {
        if(checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            myLocationEnabled(googleMap, requireActivity(), ::setCurrentLocationOnMap)
            return true
        }

        return false
    }

    private fun setCurrentLocationOnMap(location: Location)
    {
        activeMarker = if(activeMarker == null)
            setLocationMarker(location.latitude to location.longitude, googleMap, id_location, requireContext())
        else
            updateLocationMarker(location.latitude to location.longitude, googleMap, id_location, requireContext(), activeMarker!!)

        googleMap.setOnMarkerClickListener { marker -> marker != null && marker == activeMarker }
        viewModel.newUser.location = location.latitude to location.longitude
    }
}