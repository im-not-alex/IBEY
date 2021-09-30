package it.polito.mad.group33.ibey.ui.item.itemDetail

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.MapView
import com.google.android.libraries.maps.model.Marker
import it.polito.mad.group33.ibey.*
import it.polito.mad.group33.ibey.repository.firebaseRepository
import kotlinx.android.synthetic.main.item_details_fragment.*
import kotlinx.android.synthetic.main.item_show_fields.*
import kotlinx.android.synthetic.main.toolbar_show_pic.*


abstract class ItemDetailsFragment : Fragment() {
    lateinit var viewModel: ItemDetailsViewModel
    protected lateinit var mapView : MapView
    protected lateinit var googleMap: GoogleMap
    protected lateinit var locationMarker: Marker
    private var mapShown: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).populateToolbar(view.findViewById(R.id.toolbar))
        (activity as MainActivity).lockSwipeDrawer()
        //enableScrollDescription() TODO
        // resize toolbar
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            val params = toolbar_wrapper.layoutParams
            params.height = ((activity as MainActivity).availableSpace()*0.45).toInt()
            toolbar_wrapper.layoutParams = params
        }
        // carico l'immagine
        viewModel.img.observe(viewLifecycleOwner, Observer { profile_pic.setImageBitmap(it) })
        manageMapView(savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val bundle = this.arguments
        if (bundle != null) {
            val bundleItem=Item(bundle.getString(singleItemKey)!!)
            viewModel.myItem=bundleItem
        } else {
            Log.d("debug_boundle","called ItemDetails with no boundle")
            findNavController().popBackStack()
        }
        viewModel.myItem?.let { setItemInformation(it) }
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

    abstract fun setItemInformation(myItem: Item)

    protected fun getItemImage(name:String, location:String){
        firebaseRepository.getImg(name, location).addOnSuccessListener {
            Thread(Runnable {
                Glide.with(requireContext()).asBitmap().load(it).error(R.drawable.empty_picture).into(object : CustomTarget<Bitmap>(){
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        // Save image on viewmodel.livedata
                        viewModel.img.postValue(resource)
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {}})}).start()
        }.addOnFailureListener {
        }
    }

    protected fun isGoogleMapInitialized(): Boolean = this::googleMap.isInitialized
    protected fun isLocationMarkerInitialized(): Boolean = this::locationMarker.isInitialized
    protected open fun extraMapViewExpandingManagement() {}
    protected open fun extraMapViewCollapsingManagement() {}

    private fun manageMapView(savedInstanceState: Bundle?)
    {
        requireActivity().findViewById<MapView>(R.id.idShowItemLocationMap).visibility = View.GONE
        if(viewModel.myItem?.location?.first ?: 91.0 <= 90.0 && viewModel.myItem?.location?.second ?: 181.0 <= 180.0) {
            setLocationText(viewModel.myItem!!.location.first, viewModel.myItem!!.location.second, textItemLocation, requireContext())
            textItemLocation.setOnClickListener { onClickListenerShowUnShowMap(savedInstanceState) }
            idItemLocation.setOnClickListener { onClickListenerShowUnShowMap(savedInstanceState) }
        } else {
            idItemLocation.visibility = View.GONE
        }
    }

    private fun onClickListenerShowUnShowMap(savedInstanceState: Bundle?)
    {
        val mFabOpenAnim : Animation = AnimationUtils.loadAnimation(requireActivity(), R.anim.fab_open)
        val mFabCloseAnim : Animation = AnimationUtils.loadAnimation(requireActivity(), R.anim.fab_close)
        if (!mapShown) {
            if(!this::mapView.isInitialized) {
                mapView = requireActivity().findViewById(R.id.idShowItemLocationMap)
                mapView.onCreate(savedInstanceState)
            }
            else
                mapView.onResume()


            mapView.getMapAsync { googleMap ->
                this.googleMap = googleMap
                locationMarker = setLocationMarker(viewModel.myItem!!.location, googleMap, textItemLocation, requireContext())
                idItemLocation.helperText = getString(R.string.hideMap)

                val endIcon = resources.getDrawable(R.drawable.ic_keyboard_arrow_up, null)
                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    endIcon.setTint(resources.getColor(R.color.primaryDarkColor, null))
                else
                    endIcon.setTint(resources.getColor(R.color.primaryDarkColor))
                idItemLocation.endIconDrawable = endIcon

                extraMapViewExpandingManagement()
            }

            expand(mapView)

        } else {
            idItemLocation.helperText = getString(R.string.showInMap)

            val endIcon = resources.getDrawable(R.drawable.ic_keyboard_arrow_down, null)
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                endIcon.setTint(resources.getColor(R.color.primaryDarkColor, null))
            else
                endIcon.setTint(resources.getColor(R.color.primaryDarkColor))
            idItemLocation.endIconDrawable = endIcon
            extraMapViewCollapsingManagement()
            collapse(mapView)

        }
        mapShown = !mapShown
    }



//    private fun enableScrollDescription() {
//        text_item_description.setOnTouchListener(View.OnTouchListener { v, event ->
//            if (text_item_description.hasFocus()) {
//                v.parent.requestDisallowInterceptTouchEvent(true)
//                when (event.action and MotionEvent.ACTION_MASK) {
//                    MotionEvent.ACTION_SCROLL -> {
//                        v.parent.requestDisallowInterceptTouchEvent(false)
//                        return@OnTouchListener true
//                    }
//                }
//            }
//            false
//        })
//    }



}
