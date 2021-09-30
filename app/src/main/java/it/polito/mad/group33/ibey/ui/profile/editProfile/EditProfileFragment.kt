package it.polito.mad.group33.ibey.ui.profile.editProfile

import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.MapView
import com.google.android.libraries.maps.model.Marker
import com.google.android.libraries.maps.model.MarkerOptions
import it.polito.mad.group33.ibey.*
import it.polito.mad.group33.ibey.ui.edits.EditFragment
import it.polito.mad.group33.ibey.ui.profile.UserViewModel
import kotlinx.android.synthetic.main.edit_profile_fragment.*
import kotlinx.android.synthetic.main.profile_edit_fields.*


class EditProfileFragment : EditFragment() {
    private lateinit var viewModel: EditProfileViewModel
    private lateinit var edittexts:List<View>
    private lateinit var userViewModel:UserViewModel
    lateinit var mapView : MapView
    lateinit var googleMap: GoogleMap
    var activeMarker: Marker? = null

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
        (activity as MainActivity).populateToolbar(view.findViewById(R.id.toolbar))
        (activity as MainActivity).lockSwipeDrawer()

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
                            findNavController().popBackStack()
                            viewModel.dialogDismiss = true
                            loadingDialog.dismiss()
                        }
                } else { // else senza update immagine
                    Toast.makeText(requireContext(),getString(R.string.toastProfileSaved),Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                    viewModel.dialogDismiss = true
                    loadingDialog.dismiss()
                }

            }.addOnFailureListener{
                viewModel.dialogDismiss = true
                loadingDialog.dismiss()
            }

            // TODO navigare

        } else {
            //menuEnabled=false
            //requireActivity().invalidateOptionsMenu()
            Toast.makeText(requireContext(),getString(R.string.toastErrorCheck),Toast.LENGTH_SHORT).show()
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
            mapView.onResume()
        }
    }
}