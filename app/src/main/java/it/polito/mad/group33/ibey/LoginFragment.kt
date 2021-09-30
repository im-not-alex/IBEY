package it.polito.mad.group33.ibey

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.ktx.Firebase
import it.polito.mad.group33.ibey.repository.firebaseRepository
import it.polito.mad.group33.ibey.ui.profile.UserViewModel
import kotlinx.android.synthetic.main.login_fragment.*
import kotlin.math.sign


class LoginFragment : Fragment() {
    private lateinit var userViewModel: UserViewModel
    private val rcSignIn = 420
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.login_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        sign_in_button.setSize(SignInButton.SIZE_WIDE)
        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)
        //size of nav backstack printed in onsalelistfragment and loginfragment
        Log.d("debug","stack size:"+(activity as MainActivity).supportFragmentManager.findFragmentById(R.id.nav_host_fragment)?.childFragmentManager?.backStackEntryCount.toString())
        sign_in_button.setOnClickListener {  googleSignIn() }
        if(this.arguments==null){
            googleSignIn()
            profileSetup()
        }
        else
            signOut()
    }




    private fun profileSetup() {
        userViewModel.user.value = User()   // first Profile initialization
    }

    private fun googleSignIn(){
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        userViewModel.googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        if(Firebase.auth.currentUser!=null) {
            userViewModel.googleSignInClient.silentSignIn().addOnCompleteListener { task ->
                if (task.isSuccessful){
                    try {
                        val account = task.getResult(ApiException::class.java)!!
                        Log.d("signin", "firebaseAuthWithGoogle:" + account.id)
                        firebaseAuthWithGoogle(account)
                    } catch (e: ApiException) {
                        textSignin.visibility = View.INVISIBLE
                        pgsBar.visibility = View.INVISIBLE
                        sign_in_button.visibility=View.VISIBLE
                        Toast.makeText(requireContext(), getString(R.string.toastLoginErr), Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            textSignin.visibility = View.INVISIBLE
            pgsBar.visibility = View.INVISIBLE
            sign_in_button.visibility=View.VISIBLE
            sign_in_button.setOnClickListener {  signIn() }
        }
    }

    private fun signIn() {
        val signInIntent = userViewModel.googleSignInClient.signInIntent
        startActivityForResult(signInIntent, rcSignIn)
    }

    private fun signOut() {   // mantenere passaggio view!
        userViewModel.img.value=null
        Firebase.auth.signOut()
        userViewModel.googleSignInClient.signOut()
            .addOnCompleteListener(requireActivity()) {
                Toast.makeText(requireContext(), getString(R.string.toastLogout), Toast.LENGTH_LONG).show()
                findNavController().popBackStack(R.id.loginFragment,false)
                //requireActivity().finishAffinity()
                // startActivity(Intent(activity, MainActivity::class.java).apply { this.flags =
                //    Intent.FLAG_ACTIVITY_CLEAR_TASK })
                profileSetup()
                googleSignIn()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == rcSignIn) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val pgsBar:ProgressBar = requireView().findViewById(R.id.pgsBar)
            val textSignin:View = requireView().findViewById(R.id.textSignin)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                Log.d("signin", "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                //signIn()
                textSignin.visibility = View.INVISIBLE
                pgsBar.visibility = View.INVISIBLE
                sign_in_button.visibility=View.VISIBLE
                Toast.makeText(requireContext(), getString(R.string.toastMandatoryLogin), Toast.LENGTH_LONG).show()
                Log.d("signin", "Google sign in failed", e)
            }

        }

    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        textSignin.visibility = View.VISIBLE
        pgsBar.visibility = View.VISIBLE
        sign_in_button.visibility=View.INVISIBLE
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        Firebase.auth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful) {
                saveNotificationToken()
                val isNew = it.result.additionalUserInfo?.isNewUser?:false
                userViewModel.loadUserFromFirebase(Firebase.auth.currentUser?.uid?:"",true)
                userViewModel.user.observe(viewLifecycleOwner, object : Observer<User>{
                    override fun onChanged(t: User?) {
                        Log.d("UserToken",userViewModel.user.value!!.uid + " token: ")
                        if(userViewModel.user.value!!.uid!="" && userViewModel.user.value!!.fullName!="" && userViewModel.user.value!!.email!="") {
                            if(isNew) {
                                findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToEditProfileFragment())
                            } else
                                findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToOnSaleListFragment())
                            userViewModel.user.removeObserver(this)
                            Toast.makeText(requireContext(),getString(R.string.toastLoginOk) + " " + acct.email + "!", Toast.LENGTH_LONG).show()
                        }
                    }

                })
            } else {
                textSignin.visibility = View.INVISIBLE
                pgsBar.visibility = View.INVISIBLE
                sign_in_button.visibility=View.VISIBLE
                Toast.makeText(requireContext(), getString(R.string.toastLoginErr), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveNotificationToken()
    {
        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("notificationToken", "getInstanceId failed", task.exception)
                return@OnCompleteListener
            }

            // Get new Instance ID token
            val token = task.result?.token ?: ""
            val language = getCurrentLocale(requireContext()).language
            if(token != "")
                firebaseRepository.notificationToken(token, language)
        })
    }

    override fun onDetach() {
        super.onDetach()
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    override fun onStop() {
        super.onStop()
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}



