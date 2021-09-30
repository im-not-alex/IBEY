package it.polito.mad.group33.ibey.ui.edits

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputFilter
import android.text.Spanned
import android.util.DisplayMetrics
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.load.resource.bitmap.TransformationUtils.rotateImage
import it.polito.mad.group33.ibey.*
import it.polito.mad.group33.ibey.viewmodel.BundleAwareViewModelFactory
import kotlinx.android.synthetic.main.image_popup_rotate.*
import kotlinx.android.synthetic.main.toolbar_show_pic.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

open class ImageHandlerFragment : Fragment() {
    private val requestTakePhoto = 1
    private val requestOpenGallery = 2
    private val myPermissionRequestCamera = 100
    private val myPermissionRequestOpenGallery = 200

    protected lateinit var dialogImg : ImageRotateDialog
    protected lateinit var viewModelImageHandler: ViewModelImageHandlerFragment


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModelImageHandler = ViewModelProvider(this,BundleAwareViewModelFactory(savedInstanceState, ViewModelProvider.NewInstanceFactory())).get(
            ViewModelImageHandlerFragment::class.java)
    }

        protected fun showPopup(view: View) {
        val wrapper: Context = ContextThemeWrapper(requireContext(),
            R.style.PopupMenu
        )
        PopupMenu(wrapper, view).apply {

            setOnMenuItemClickListener { item: MenuItem? ->
                when (item!!.itemId) {
                    R.id.takeapic -> {
                        dispatchTakePictureIntentWithCheckPermission()
                    }
                    R.id.opengall -> {
                        dispatchOpenGalleryIntentWithCheckPermission()
                    }
                }

                true
            }
            inflate(R.menu.editprofile_context_menu)
            val fieldMpPopup = PopupMenu::class.java.getDeclaredField("mPopup")
            fieldMpPopup.isAccessible = true
            val mPopup = fieldMpPopup.get(this)
            mPopup.javaClass
                .getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                .invoke(mPopup, true)
            show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            myPermissionRequestCamera -> {
                if (grantResults.isNotEmpty() && grantResults.first() == PackageManager.PERMISSION_GRANTED) {
                    Log.i("ibey", "Permission (Camera) allowed by user")
                    dispatchTakePictureIntent()
                } else
                    Log.i("ibey", "Permission (Camera) denied by user")
            }
            myPermissionRequestOpenGallery -> {
                if (grantResults.isNotEmpty() && grantResults.first() == PackageManager.PERMISSION_GRANTED) {
                    Log.i("ibey", "Permission (Open Gallery) allowed by user")
                    dispatchOpenGalleryIntent()
                } else
                    Log.i("ibey", "Permission (Open Gallery) denied by user")
            }
        }
    }

//##############################
//      Open Gallery functions
//##############################

    private fun dispatchOpenGalleryIntentWithCheckPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        )
            requestPermissions(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                myPermissionRequestOpenGallery
            )
        else {
            Log.i("ibey", "Permission previously allowed by user")
            dispatchOpenGalleryIntent()
        }
    }

    private fun dispatchOpenGalleryIntent() {
        val intent = Intent(Intent.ACTION_PICK)
        //val intent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        //intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, requestOpenGallery)
    }

    //##############################
    //      Open Camera functions
    //##############################
    private fun dispatchTakePictureIntentWithCheckPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        )
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                myPermissionRequestCamera
            )
        else {
            Log.i("ibey", "Permission previously allowed by user")
            dispatchTakePictureIntent()
        }
    }

    private fun dispatchTakePictureIntent() {
        val packageManager = (activity as MainActivity).packageManager
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                viewModelImageHandler.tempimg = createImageFile()
                viewModelImageHandler.tempimg?.also {
                    val photoURI = FileProvider.getUriForFile(
                        requireContext(),
                        "it.polito.mad.group33.ibey.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, requestTakePhoto)
                }
            }
        }
    }

    @Throws(IOException::class)
    protected fun createImageFile(): File {
        return if (viewModelImageHandler.tempimg == null) {
            // Create an image file name
            File((activity as MainActivity).filesDir,
                tempPic
            )
        } else {
            viewModelImageHandler.tempimg!!
        }
    }

//##############################
//      Result functions
//##############################

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            requestTakePhoto -> {
                if (resultCode != AppCompatActivity.RESULT_CANCELED) {
                    val rotatedBitmap = rotateBitMapFromImgPath(viewModelImageHandler.tempimg!!.absolutePath)
                    val centerBitmap=centerCrop(rotatedBitmap)
                    profile_pic.setImageBitmap(centerBitmap)
                    viewModelImageHandler.img.value = centerBitmap
                    viewModelImageHandler.imgModified = true
                    requireActivity().invalidateOptionsMenu()
                }
            }
            requestOpenGallery -> {
                if (resultCode == Activity.RESULT_OK) {
                    viewModelImageHandler.tempimg = createImageFile()
                    //val absoluteImgPath = cursorSupport(data?.data!!)
                    //EXIF STREAM
                    val rotatedBitmap = rotateBitMapFromImgStream(data?.data!!)
                    val centerBitmap=centerCrop(rotatedBitmap)
                    profile_pic.setImageBitmap(centerBitmap)
                    viewModelImageHandler.img.value = centerBitmap
                    val stream: OutputStream = FileOutputStream(viewModelImageHandler.tempimg!!)
                    viewModelImageHandler.img.value?.compress(
                        Bitmap.CompressFormat.JPEG,
                        100,
                        stream
                    )
                    stream.flush()
                    stream.close()
                    viewModelImageHandler.imgModified = true
                    requireActivity().invalidateOptionsMenu()
                }
            }
        }
    }

//##############################
//      Rotate functions
//##############################


    private fun rotateBitMapFromImgPath(path: String): Bitmap {
        val imageBitmap = BitmapFactory.decodeFile(path)
        val exif = ExifInterface(path)
        return applyExif(exif, imageBitmap)
    }

    private fun rotateBitMapFromImgStream(path: Uri): Bitmap {
        val inp = requireActivity().contentResolver.openInputStream(path)
        val imageBitmap = BitmapFactory.decodeStream(inp)
        val exif = ExifInterface(inp!!)
        return applyExif(exif, imageBitmap)
    }

    private fun applyExif(exif: ExifInterface, imageBitmap: Bitmap): Bitmap {
        val orientation: Int = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )
        viewModelImageHandler.imgRotateDegree = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            ExifInterface.ORIENTATION_NORMAL -> 0
            else -> 0
        }
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(
                imageBitmap,
                90
            )
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(
                imageBitmap,
                180
            )
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(
                imageBitmap,
                270
            )
            ExifInterface.ORIENTATION_NORMAL -> imageBitmap
            else -> imageBitmap
        }!!
    }
    protected fun dialogBuild(){
        viewModelImageHandler.img.observe(
            viewLifecycleOwner,
            androidx.lifecycle.Observer { profile_pic.setImageBitmap(it) })

        dialogImg =
            ImageRotateDialog(
                requireActivity()
            )
        profile_pic.setOnClickListener { dialogImg.show(viewModelImageHandler.img.value) }
        dialogImg.setOnDismissListener {
            if(dialogImg.rotationAngle!=0) {
                viewModelImageHandler.imgRotateDegree += dialogImg.rotationAngle
                viewModelImageHandler.img.value =
                    rotateImage(
                        viewModelImageHandler.img.value!!,
                        dialogImg.rotationAngle
                    )
                viewModelImageHandler.imgModified=true
                requireActivity().invalidateOptionsMenu()
            }
        }
    }

    class ImageRotateDialog(private var c: Activity) : Dialog(c),
        View.OnClickListener {
        private var sizes = DisplayMetrics()
        private lateinit var imgBitmap: Bitmap
        var rotationAngle=0

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.image_popup_rotate)
            confirm_button.setOnClickListener(this)
            cancel_button.setOnClickListener(this)
            left_rotate.setOnClickListener(this)
            right_rotate.setOnClickListener(this)
            c.windowManager.defaultDisplay.getMetrics(sizes)
            val pxSize: Int = if (sizes.heightPixels > sizes.widthPixels)
                sizes.widthPixels
            else
                sizes.heightPixels
            id_dialog.layoutParams.width = pxSize
            setCanceledOnTouchOutside(true)
        }

         fun show(img:Bitmap?) {
             if (img != null){
                 super.show()
                 rotationAngle = 0
                 imgBitmap = getBitmapFromView(c.profile_pic)
                 pic_edit.setImageBitmap(imgBitmap)
         }
        }

        override fun onClick(v: View) {
            when (v.id) {
                R.id.confirm_button -> dismiss()
                R.id.cancel_button -> cancel()
                R.id.left_rotate -> {
                    imgBitmap = rotateImage(
                        imgBitmap,
                        -90
                    )!!
                    rotationAngle-=90
                    rotationAngle%=360
                    pic_edit.setImageBitmap(imgBitmap)

                }
                R.id.right_rotate -> {
                    imgBitmap = rotateImage(
                        imgBitmap,
                        +90
                    )!!
                    rotationAngle+=90
                    rotationAngle%=360
                    pic_edit.setImageBitmap(imgBitmap)
                }
                else -> cancel()
            }
        }

        private fun getBitmapFromView(view: View): Bitmap {
            val bitmap =
                Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            return bitmap
        }

        override fun cancel() {
            rotationAngle=0
            super.cancel()
        }

    }

    val filter: InputFilter = object : InputFilter {
        override fun filter(
            source: CharSequence, start: Int, end: Int,
            dest: Spanned?, dstart: Int, dend: Int
        ): CharSequence? {
            for (i in start until end) {
                if (!(Character.isLetter(source[i]) || source[i] == ' ' || source[i] == '-' || source[i] == '\'')) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.toastinput),
                        Toast.LENGTH_SHORT).show()
                    return source.replace(textRegex, "")
                }
            }
            return null
        }
    }

    fun rotateImage(source: Bitmap, angle: Int): Bitmap? {
        val matrix = Matrix()
        if (angle == 0)
            return source
        matrix.postRotate(angle.toFloat())
        viewModelImageHandler.imgModified=true
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        )
    }

    fun centerCrop(srcBmp:Bitmap) :Bitmap{
         val dstbtm=if (srcBmp.width >= srcBmp.height){

            Bitmap.createBitmap(
                srcBmp,
                srcBmp.width /2 - srcBmp.height /2,
                0,
                srcBmp.height,
                srcBmp.height
            )
        }else{
            Bitmap.createBitmap(
                srcBmp,
                0,
                srcBmp.height /2 - srcBmp.width /2,
                srcBmp.width,
                srcBmp.width
            )
        }
        return Bitmap.createScaledBitmap(dstbtm, 1080, 1080, false)
    }

}