package it.polito.mad.group33.ibey

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.navigation.NavigationView
import it.polito.mad.group33.ibey.repository.firebaseRepository.getImg
import it.polito.mad.group33.ibey.ui.edits.EditFragment
import it.polito.mad.group33.ibey.ui.item.boughtItemsListFragment.BoughtItemsListFragment
import it.polito.mad.group33.ibey.ui.item.itemList.ItemListFragment
import it.polito.mad.group33.ibey.ui.item.itemOnSaleList.OnSaleListFragment
import it.polito.mad.group33.ibey.ui.item.itemsOfInterestListFragment.ItemsOfInterestListFragment
import it.polito.mad.group33.ibey.ui.profile.UserViewModel
import it.polito.mad.group33.ibey.ui.profile.showProfile.OwnProfileFragment
import it.polito.mad.group33.ibey.viewmodel.BundleAwareViewModelFactory
import kotlinx.android.synthetic.main.nav_header_main.*


class MainActivity : AppCompatActivity() {
    lateinit var userViewModel:UserViewModel
    private lateinit var appBarConfiguration: AppBarConfiguration
    lateinit var mainHandler: Handler
    private var doubleBackToExitPressedOnce = false

    private val tokenUpdater = object : Runnable {
        override fun run() {
            userViewModel.keepTokenAlive()
            //val millis:Long = userViewModel.firebaseRepository.tokenTimeExp*1000 - System.currentTimeMillis()
            mainHandler.postDelayed(this, 40*60*1000) // if User keeps app open for more than 40 mins
        }
    }

//    private val imgUpdater = object : Runnable { url ->
//        override fun run() {
//            userViewModel.img.value= Glide.with(applicationContext).asBitmap().load(url).error(R.mipmap.ic_launcher_round).submit().get()
//            mainHandler.post(
//        }
//    }
    // NB: al momento activity senza onSave e onRestore perchè sono dati statici ripresi da memoria
    // potrebbe essere necessario farlo per altri dati!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        categoryMap = resources.getStringArray(R.array.category).mapIndexed{ index, s -> (s to resources.getStringArray(resources.getIdentifier("category_$index","array",packageName)))}.toMap()
        statusArray = resources.getStringArray(R.array.itemStatus)
        heightMap = resources.getDimension(R.dimen.locationMapViewHeight).toInt()
        userViewModel = ViewModelProvider(this,BundleAwareViewModelFactory(savedInstanceState, ViewModelProvider.NewInstanceFactory())).get(UserViewModel::class.java)

        setContentView(R.layout.activity_main)

        mainHandler = Handler(Looper.getMainLooper())

    }



    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        initDrawer()
        return true
    }

    override fun onPause() {
        super.onPause()
        mainHandler.removeCallbacks(tokenUpdater)

    }

    override fun onResume() {
        super.onResume()
        mainHandler.post(tokenUpdater)

    }


    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        val toBeReturned = navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
        onBackPressedForEditFragments()
        return toBeReturned
    }

    override fun onBackPressed() {
        if(onDoubleBackPressedForPrincipalFragments())
            return

        onBackPressedForEditFragments()
        super.onBackPressed()
    }

     fun populateToolbar(toolbar: Toolbar){
         // Population
         //val toolbar: Toolbar = findViewById(R.id.toolbar)
         setSupportActionBar(toolbar)
         unlockSwipeDrawer()

         val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
         val navView: NavigationView = findViewById(R.id.nav_view)
         val navController = findNavController(R.id.nav_host_fragment)
         // Passing each menu ID as a set of Ids because each
         // menu should be considered as top level destinations.

         appBarConfiguration = AppBarConfiguration(setOf(R.id.itemListFragment, R.id.OwnProfileFragment, R.id.onSaleListFragment,
             R.id.boughtItemsListFragment, R.id.itemsOfInterestListFragment), drawerLayout)
         setupActionBarWithNavController(navController, appBarConfiguration)
         navView.setupWithNavController(navController)


         navView.setNavigationItemSelectedListener { menuItem ->
             val id = menuItem.itemId
             val navOptions = NavOptions.Builder().setPopUpTo(R.id.onSaleListFragment,true).build()
             when (id) {
                 R.id.logout -> navController.navigate(R.id.loginFragment, bundleOf("firstTime" to false),navOptions)
                 else -> navController.navigate(id,null,navOptions)
             }
             drawerLayout.closeDrawer(GravityCompat.START, true)
             false // TODO check che non si debba fare false
         }

     }


    private fun unlockSwipeDrawer(){
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    }

    fun lockSwipeDrawer(){
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }



     fun availableSpace(): Int {
        // statusbarheight
        var statusBarHeight = 0
        val r1 = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (r1 > 0) {
            statusBarHeight = resources.getDimensionPixelSize(r1)
        }
        // navigation bar height
        var navigationBarHeight = 0
        val resourceId =
            resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            navigationBarHeight = resources.getDimensionPixelSize(resourceId)
        }
        Log.i("db1", "$statusBarHeight  $navigationBarHeight")
        val displayMetrics= DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        return displayMetrics.heightPixels-(statusBarHeight+navigationBarHeight)
    }


    private fun initDrawer(){
        findNavController(R.id.nav_host_fragment)
        // load user image from Firestore
        getUserImage("profile_0.jpg","Users/"+userViewModel.user.value!!.uid)

        userViewModel.img.observe(
            this, Observer { profile_pic_drawer.setImageBitmap(it) })
        userViewModel.user.observe(this, Observer {
            fullname_drawer.text = userViewModel.user.value!!.fullName
            email_drawer.text = userViewModel.user.value!!.email

        })
    }

     fun getUserImage(name:String, location:String){
        getImg(name,location).addOnSuccessListener {
            Thread(Runnable {
                Glide.with(applicationContext).asBitmap().load(it).error(R.drawable.ic_avatar).into(object : CustomTarget<Bitmap>(){
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        // Save image on viewmodel.livedata
                        userViewModel.img.postValue(resource)
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {}})}).start()
        }.addOnFailureListener {
            Log.w("Image not existing", it.message?:"")
        }
    }

    private fun onBackPressedForEditFragments()
    {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)?.childFragmentManager?.fragments?.get(0)
        if(currentFragment != null && currentFragment is EditFragment)
            currentFragment.onBackPressedAction()
    }

    private fun onDoubleBackPressedForPrincipalFragments(): Boolean
    {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)?.childFragmentManager?.fragments?.get(0)
        if(currentFragment != null && (currentFragment is ItemListFragment || currentFragment is OwnProfileFragment ||
                    currentFragment is OnSaleListFragment || currentFragment is BoughtItemsListFragment ||
                    currentFragment is ItemsOfInterestListFragment))
        {
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed()
                return true
            }

            this.doubleBackToExitPressedOnce = true
            Toast.makeText(this, getString(R.string.doubleBackClick), Toast.LENGTH_SHORT).show()

            Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
            return true
        }
        return false
    }
}
