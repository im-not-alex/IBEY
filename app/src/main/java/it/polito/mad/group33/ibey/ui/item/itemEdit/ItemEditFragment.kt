package it.polito.mad.group33.ibey.ui.item.itemEdit


import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.MapView
import com.google.android.libraries.maps.model.Marker
import com.google.android.libraries.maps.model.MarkerOptions
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import it.polito.mad.group33.ibey.*
import it.polito.mad.group33.ibey.repository.firebaseRepository
import it.polito.mad.group33.ibey.ui.edits.EditFragment
import it.polito.mad.group33.ibey.ui.item.itemDetail.ItemDetailsViewModel
import it.polito.mad.group33.ibey.ui.profile.UserViewModel
import it.polito.mad.group33.ibey.viewmodel.BundleAwareViewModelFactory
import kotlinx.android.synthetic.main.item_edit_fields.*
import kotlinx.android.synthetic.main.item_edit_fields.id_item_date
import kotlinx.android.synthetic.main.item_edit_fields.id_item_location
import kotlinx.android.synthetic.main.item_edit_fields.id_item_price
import kotlinx.android.synthetic.main.item_edit_fields.id_item_title
import kotlinx.android.synthetic.main.item_edit_fields.text_item_date
import kotlinx.android.synthetic.main.item_edit_fields.text_item_description
import kotlinx.android.synthetic.main.item_edit_fields.text_item_location
import kotlinx.android.synthetic.main.item_edit_fields.text_item_price
import kotlinx.android.synthetic.main.item_edit_fields.text_item_title
import kotlinx.android.synthetic.main.item_edit_fragment.*
import kotlinx.android.synthetic.main.profile_edit_fields.*
import kotlinx.android.synthetic.main.toolbar_show_pic.*
import java.text.DecimalFormat
import java.util.*


class ItemEditFragment : EditFragment() {
    private lateinit var viewModel: ItemEditViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var itemDetailsViewModel: ItemDetailsViewModel
    private lateinit var edittexts:List<View>
    private lateinit var materialDatePickerBuilder:MaterialDatePicker<Long>
    private val LOCATION_PERMISSION_ID = 42
    private var state = 0
    lateinit var mapView : MapView
    lateinit var googleMap: GoogleMap
    var activeMarker: Marker? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        viewModel = ViewModelProvider(this,BundleAwareViewModelFactory(savedInstanceState, ViewModelProvider.NewInstanceFactory())).get(ItemEditViewModel::class.java)
        val view = inflater.inflate(R.layout.item_edit_fragment, container, false)
        manageMapView(view, savedInstanceState)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // Populating Toolbar w/ Drawer features
        (activity as MainActivity).populateToolbar(view.findViewById(R.id.toolbar))
        (activity as MainActivity).lockSwipeDrawer()
        // For scrolling inside Description
        enableScrollDescription()
        // configurazione e building del Date Picker
        confAndBuildDatePicker()

        // Popup FAB
        id_takepic.setOnClickListener { showPopup(it) }

        categoryDropDown()

        // resize toolbar
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            val params = toolbar_wrapper.layoutParams
            params.height = ((activity as MainActivity).availableSpace() * 0.45).toInt()
            toolbar_wrapper.layoutParams = params
        }
        edittexts= listOf(text_item_title,text_item_price,text_item_location,text_item_date,filled_exposed_dropdown)
        for(elem in edittexts)
            elem.setOnFocusChangeListener { v, hasFocus ->
                if(hasFocus) checkCompiledForms(v,clear = true)
                else checkCompiledForms(v)}

        id_item_location.setEndIconOnClickListener {
            text_item_location.setText("")
            activeMarker?.isVisible = false
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)
        itemDetailsViewModel = ViewModelProvider(requireActivity()).get(ItemDetailsViewModel::class.java)

        viewModelImageHandler.img.observe(viewLifecycleOwner, Observer { profile_pic.setImageBitmap(it) })

        dialogBuild()

        // Bundle management, ricevo item da modificare
        val bundle = this.arguments
        if (bundle != null) {
            viewModel.myItem=Item(bundle.getString(singleItemKey)!!)
            if(viewModel.myItem.id!="") {
                profile_pic.setImageBitmap(itemDetailsViewModel.img.value)
            }

            // fix ritorno a details alla prima creazione: serve id user popolato
            if (viewModel.myItem.seller!!.uid =="") {
                viewModel.myItem.seller!!.uid = userViewModel.user.value?.uid ?: ""
                viewModel.myItem.seller!!.nickname = userViewModel.user.value?.uid ?: ""
            }
            viewModel.newItem=viewModel.myItem.copy()
        } else {
            Log.d("debug_boundle", "called ItemEdit with no boundle")
            findNavController().popBackStack()
        }

        // al primo ingresso e ai successivi ingressi usa view model / bundle andando a popolare con l'item temporaneamente modificato
        setItemInformationOnInputFields(viewModel.myItem) // popolo i fields con l'item iniziale

        if (!viewModelImageHandler.imgModified)
            getItemImage("item_0.jpg","Users/"+viewModel.myItem.seller!!.uid + "/" + viewModel.myItem.id)

        viewModelImageHandler.menuEnabled = true // Tasto Save attivo di default
        
        editHiddenSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.newItem.statusInt = if(isChecked) 2 else 0
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.newItem = retrieveInsertedItem()
//        viewModel.menuEnabled=menuEnabled
//        viewModel.imgModified=imgModified
//        viewModel.tempimg=tempimg
//        viewModel.imgRotateDegree=imgRotateDegree

        viewModel.writeTo(outState)
        viewModelImageHandler.writeTo(outState)

        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        dialogImg.cancel()
        if(materialDatePickerBuilder.isVisible)
            materialDatePickerBuilder.dismiss()

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

    private fun categoryDropDown() {
        val categoryAndSubCategoryMapKeys = categoryMap.keys.toTypedArray()
        val adapter = ArrayAdapter(requireContext(),R.layout.dropdown_menu_popup_item,categoryAndSubCategoryMapKeys)
        // prima popolazione
        state = 0
        var text = ""
        var catInt = -1
        filled_exposed_dropdown.inputType = InputType.TYPE_NULL // disable keyboard
        filled_exposed_dropdown.setAdapter(adapter)

        filled_exposed_dropdown.setOnClickListener {
            hideKeyboard()
        }

        filled_exposed_dropdown.setOnItemClickListener { _, _, position, _ ->
            if (state == 0) {
                // selezionato la categoria
                text = categoryAndSubCategoryMapKeys[position]
                catInt = position

                filled_exposed_dropdown.setOnDismissListener {
                    if (state == 1) {
                        state = 2
                        filled_exposed_dropdown.requestFocus()
                        filled_exposed_dropdown.showDropDown()
                    } else {
                        if (state == 2) {
                            filled_exposed_dropdown.setText(viewModel.myItem.category)
                        }
                        state = 0
                        filled_exposed_dropdown.setAdapter(
                            ArrayAdapter(
                                requireContext(),
                                R.layout.dropdown_menu_popup_item,
                                categoryAndSubCategoryMapKeys
                            )
                        )
                        removeFocus()
                    }
                }

                filled_exposed_dropdown.setAdapter(
                    ArrayAdapter(
                        requireContext(),
                        R.layout.dropdown_menu_popup_item,
                        categoryMap[text]
                            ?: error(message = "Missing category in map")
                    )
                )
                state = 1
            } else if (state == 2) {
                // selezionato la sottocategoria
                state = 3
                // retrieve from text view
                Log.d("teeeeee", "$text ${filled_exposed_dropdown.text}")
                text += " - " + filled_exposed_dropdown.text
                filled_exposed_dropdown.setText(text)
                viewModel.newItem.category=text
                viewModel.newItem.categoryInt= catInt to position
            }
        }
    }

    private fun removeFocus() {
        val current: View? = requireActivity().currentFocus
        current?.clearFocus()
    }

    private fun enableScrollDescription() {
        text_item_description.setOnTouchListener(OnTouchListener { v, event ->
            if (text_item_description.hasFocus()) {
                v.parent.requestDisallowInterceptTouchEvent(true)
                when (event.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_SCROLL -> {
                        v.parent.requestDisallowInterceptTouchEvent(false)
                        return@OnTouchListener true
                    }
                }
            }
            false
        })
    }

    private fun confAndBuildDatePicker() {
        val locale: Locale = getCurrentLocale(requireContext())
        Locale.setDefault(locale)
        val today = MaterialDatePicker.todayInUtcMilliseconds()
        val constraintBuilder = CalendarConstraints.Builder()
        constraintBuilder.setStart(today)
        constraintBuilder.setValidator(DateValidatorPointForward.from(today))

        val datePickerBuilder = MaterialDatePicker.Builder.datePicker()
        datePickerBuilder.setCalendarConstraints(constraintBuilder.build())

        datePickerBuilder.setTitleText(R.string.item_date)
        materialDatePickerBuilder = datePickerBuilder.build()
        materialDatePickerBuilder.addOnDismissListener { checkCompiledForms((text_item_date)) }
        materialDatePickerBuilder.isCancelable=true
        var runnable = true
        text_item_date.setOnClickListener {
            // open date picker
            if (runnable) {
                id_item_date.error=null
                materialDatePickerBuilder.show(
                    (activity as MainActivity).supportFragmentManager,
                    "DATE_PICKER"
                )
                runnable = false
            }

        }
        materialDatePickerBuilder.addOnPositiveButtonClickListener {
            viewModel.newItem.expiryDate = it // memorizzo valore attuale preso dall'utente (indiretto per newItem)
            viewModel.newItem.expiryDateString= materialDatePickerBuilder.headerText
            text_item_date.setText(materialDatePickerBuilder.headerText)
            runnable = true
        }
        materialDatePickerBuilder.addOnCancelListener { runnable = true }
        materialDatePickerBuilder.addOnDismissListener { runnable = true }
        materialDatePickerBuilder.addOnNegativeButtonClickListener { runnable = true }
    }

    private fun setItemInformationOnInputFields(myItem: Item) {
        text_item_title!!.setText(myItem.title)
        text_item_description!!.setText(myItem.description)
        if (myItem.price != -1.0) {   // default case, leave the box empty
            val decimalFormat = DecimalFormat("0.00")   // .replace(".","")
            text_item_price!!.setText(decimalFormat.format(myItem.price))
        }
        filled_exposed_dropdown!!.setText(myItem.category)
        text_item_date!!.setText(myItem.expiryDateString)
        setHiddenSwitchStatus(myItem.statusInt, editHiddenSwitch, statusHiddenLayout)
    }


    private fun getItemImage(name:String, location:String){
        firebaseRepository.getImg(name, location).addOnSuccessListener {
            Thread(Runnable {
                Glide.with(requireContext()).asBitmap().load(it).error(R.drawable.empty_picture).into(object : CustomTarget<Bitmap>(){
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        // Save image on viewmodel.livedata
                        viewModelImageHandler.img.postValue(resource)
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {}})}).start()
        }.addOnFailureListener {
            Log.e("itemEdit", it.message)
            // Handle any errors
        }
}



    private fun checkCompiledForms(view: View?, all:Boolean=false, clear :Boolean=false): Boolean {
        if(!all) {
            when (view) {
                text_item_title ->
                    if (text_item_title.text.toString().trim() == "" && !clear) {
                        id_item_title.error = getString(R.string.empty_field_error)
                        return false
                    } else id_item_title.error=null
                text_item_price ->
                    if (text_item_price.text.toString().trim() == "" && !clear) {
                        id_item_price.error = getString(R.string.empty_field_error)
                        return false
                    } else if (text_item_price.text != null && !text_item_price.text.toString()
                            .matches(priceRegex)  && !clear
                    ) {
                        id_item_price.error = getString(R.string.wrong_price_error)
                        return false
                    } else id_item_price.error=null
                text_item_location ->
                    if (text_item_location.text.toString().trim() == ""  && !clear) {
                        id_item_location.error = getString(R.string.empty_field_error)
                        return false
                    } else id_item_location.error=null
                filled_exposed_dropdown ->
                    if (filled_exposed_dropdown.text.toString().trim() == ""  && !clear) {
                        parent_dropdown.error = getString(R.string.empty_field_error)
                        return false
                    } else parent_dropdown.error = null
                text_item_date ->
                    if (text_item_date.text.toString().trim() == ""  && !clear) {
                        id_item_date.error = getString(R.string.empty_field_error)
                        return false
                    } else id_item_date.error=null
            }
        } else {
            var res = true
            for(elem in edittexts)
                res = checkCompiledForms(elem) && res
            return res
        }
        return true
    }


    override fun confirmEdits() {
        if (checkCompiledForms(null,true)) {
            viewModel.newItem = retrieveInsertedItem()
/*            // Image persistence
            // move tmp image into the confirmed image (profile_pic)
            if (viewModelImageHandler.imgModified) {
                val folderPath = requireActivity().filesDir.absolutePath
                val folder = File(folderPath, itemFolderName)
                if (!File(requireActivity().filesDir, itemFolderName).exists())
                    folder.mkdir()

                val imgfile = File(
                    (activity as MainActivity).filesDir,
                    "/" + itemFolderName + "/" + itemPicNamePrefix + viewModel.newItem.id + itemPicNameExtension
                )
                // Compress the bitmap and save in jpg format
                val stream = FileOutputStream(imgfile)
                viewModelImageHandler.img.value?.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                stream.flush()
                stream.close()
                viewModelImageHandler.tempimg?.delete()
            }
            // navigate*/
            val loadingDialog = LoadingDialog(requireActivity(),R.style.LoadingDialog)
            viewModel.dialogDismiss = false
            loadingDialog.setOnDismissListener { dialog ->
                if(viewModel.dialogDismiss)
                    dialog.dismiss()
            }
            loadingDialog.show()
//            if(viewModel.newItem.id=="")
//                Toast.makeText(requireContext(),"Creating Item",Toast.LENGTH_SHORT).show()
//            else
//                Toast.makeText(requireContext(),getString(R.string.toastUpdating),Toast.LENGTH_LONG).show()

            viewModel.saveItemToFirebase().addOnSuccessListener {
                viewModel.newItem.id = it
                val newFlag:Boolean = viewModel.myItem.id==""
                if(viewModelImageHandler.imgModified){
                    viewModel.saveImagetoStorage(viewModelImageHandler.img.value!!)
                    .addOnFailureListener {
                        Log.e("itemEdit", it.message)
                        viewModel.dialogDismiss = true
                        loadingDialog.dismiss()
                    }
                    .addOnSuccessListener {
                        if(!newFlag)
                            Toast.makeText(requireContext(),R.string.toastItemSaved,Toast.LENGTH_SHORT).show()
                        itemDetailsViewModel.img.value=viewModelImageHandler.img.value!!
                        itemDetailsViewModel.imgModified = true
                        val bundle = bundleOf(singleItemKey to viewModel.newItem.toString(),newFlagKey to newFlag)
                        findNavController().navigate(R.id.action_itemEditFragment_to_OwnItemDetailsFragment, bundle)
                        viewModel.dialogDismiss = true
                        loadingDialog.dismiss()
                    }
                } else {
                    if(!newFlag)
                        Toast.makeText(requireContext(),R.string.toastItemSaved,Toast.LENGTH_SHORT).show()
                    val bundle = bundleOf(singleItemKey to viewModel.newItem.toString(),newFlagKey to newFlag)
                    findNavController().navigate(R.id.action_itemEditFragment_to_OwnItemDetailsFragment, bundle)
                    viewModel.dialogDismiss = true
                    loadingDialog.dismiss()
                }

            }.addOnFailureListener{
                Log.e("itemEdit", it.message)
                viewModel.dialogDismiss = true
                loadingDialog.dismiss()
            }

            // TODO navigare
            //viewModel.uploadFile(imgfile)

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

    private fun retrieveInsertedItem(): Item {
        // Take all the inputs from the user
        var tmpPrice = -1.0
        if (text_item_price.text.toString() != "")
            tmpPrice = text_item_price.text.toString().trim().replace(",", ".").toDouble()
        return Item(
            viewModel.myItem.id,
            text_item_title.text.toString().trim(),
            text_item_description.text.toString().trim(),
            tmpPrice,
            viewModel.newItem.category,
            viewModel.newItem.location,
            viewModel.newItem.expiryDate,
            viewModel.newItem.expiryDateString,
            viewModel.myItem.seller,
            viewModel.newItem.status,
            viewModel.newItem.statusInt,
            viewModel.newItem.categoryInt
        )
    }

    private fun manageMapView(view: View, savedInstanceState: Bundle?)
    {
        mapView = view.findViewById(R.id.idEditItemLocationMap)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { googleMap ->
            this.googleMap = googleMap

            if(viewModel.myItem.location.first <= 90.0 && viewModel.myItem.location.second <= 180) {
                activeMarker = setLocationMarker(viewModel.myItem.location, googleMap, text_item_location, requireContext())
            }

            googleMap.setOnMapClickListener { latLng ->
                if(activeMarker == null) {
                    activeMarker = googleMap.addMarker(MarkerOptions().position(latLng))
                }
                else
                    activeMarker!!.position = latLng

                activeMarker!!.isVisible = true
                text_item_location.setText(getAddressFromCoordinates(latLng.latitude, latLng.longitude, requireContext()))
                viewModel.newItem.location = latLng.latitude to latLng.longitude
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
            setLocationMarker(location.latitude to location.longitude, googleMap, text_item_location, requireContext())
        else
            updateLocationMarker(location.latitude to location.longitude, googleMap, text_item_location, requireContext(), activeMarker!!)

        googleMap.setOnMarkerClickListener { marker -> marker != null && marker == activeMarker }
        viewModel.newItem.location = location.latitude to location.longitude
    }
}
