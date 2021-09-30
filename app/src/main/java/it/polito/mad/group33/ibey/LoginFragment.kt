package it.polito.mad.group33.ibey

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.iid.FirebaseInstanceId
import it.polito.mad.group33.ibey.repository.firebaseRepository
import it.polito.mad.group33.ibey.ui.profile.UserViewModel
import java.io.File


class LoginFragment : Fragment() {
    private lateinit var userViewModel: UserViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.login_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)
        //size of nav backstack printed in onsalelistfragment and loginfragment
        Log.d("debug","stack size:"+(activity as MainActivity).supportFragmentManager.findFragmentById(R.id.nav_host_fragment)?.childFragmentManager?.backStackEntryCount.toString())

        if(this.arguments==null){
            googleSignIn()
            val pgsBar:ProgressBar = requireView().findViewById(R.id.progressBar)
            val textSignin:View = requireView().findViewById(R.id.textSignin)
            textSignin.visibility = View.INVISIBLE
            pgsBar.visibility = View.INVISIBLE
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
        userViewModel.auth = FirebaseAuth.getInstance()
        signIn()

    }

    private fun signIn() {
        val signInIntent = userViewModel.googleSignInClient.signInIntent
        startActivityForResult(signInIntent, userViewModel.rcSignIn)
    }

    private fun signOut() {   // mantenere passaggio view!
        userViewModel.auth!!.signOut()
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
        if (requestCode == userViewModel.rcSignIn) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val pgsBar:ProgressBar = requireView().findViewById(R.id.progressBar)
            val textSignin:View = requireView().findViewById(R.id.textSignin)
            textSignin.visibility = View.VISIBLE
            pgsBar.visibility = View.VISIBLE
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                Log.d("signin", "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                signIn()
                Toast.makeText(requireContext(), getString(R.string.toastMandatoryLogin), Toast.LENGTH_LONG).show()
                Log.d("signin", "Google sign in failed", e)
                textSignin.visibility = View.INVISIBLE
                pgsBar.visibility = View.INVISIBLE
            }

            saveNotificationToken()
        }

    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        userViewModel.auth!!.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful) {
            //  Goolge data for other usages: acct.displayName?: "" | acct.displayName?: ""  | acct.email?:"" | auth.currentUser?.uid?:""
                // dati statici presi da google caricati, ora update dal nostro db per altri dati!
                userViewModel.loadUserFromFirebase(userViewModel.auth!!.currentUser?.uid?:"",true)
                userViewModel.user.observe(this, Observer{
                    userViewModel.keepTokenAlive();
                    Log.d("UserToken",userViewModel.user.value!!.uid + " token: " + userViewModel.usrToken.value )
                    if(userViewModel.user.value!!.uid!="" && userViewModel.usrToken.value !="") {
                        try {
                            this.findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToOnSaleListFragment())
                        }catch (Error:IllegalArgumentException){}

                        Toast.makeText(requireContext(),getString(R.string.toastLoginOk) + " " + acct.email + "!", Toast.LENGTH_LONG).show()
                    }
                })
                userViewModel.usrToken.observe(this,Observer{
                    Log.d("UserToken",userViewModel.user.value!!.uid + " token: " + userViewModel.usrToken.value )
                    if(userViewModel.user.value!!.uid!="" && userViewModel.usrToken.value !="") {
                        try {
                            this.findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToOnSaleListFragment())
                        }catch (Error:IllegalArgumentException){}
                        Toast.makeText(requireContext(),getString(R.string.toastLoginOk) + " " + acct.email + "!", Toast.LENGTH_LONG).show()
                    }
                })
            } else {
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
}



