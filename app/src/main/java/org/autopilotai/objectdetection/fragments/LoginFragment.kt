package org.autopilotai.objectdetection.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.Callback
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import com.auth0.android.result.UserProfile
import com.google.android.material.snackbar.Snackbar
import org.autopilotai.objectdetection.LoginViewModel
import org.autopilotai.objectdetection.R
import org.autopilotai.objectdetection.databinding.FragmentLoginBinding
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.Observer
import androidx.navigation.NavController


/**
 * A simple [Fragment] subclass.
 * Use the [LoginFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LoginFragment : Fragment() {

    private val TAG = "LoginFragment"

    private var _fragmentLoginBinding: FragmentLoginBinding? = null

    private val fragmentLoginBinding
        get() = _fragmentLoginBinding!!

    private lateinit var account: Auth0
    private var cachedCredentials: Credentials? = null
    private var cachedUserProfile: UserProfile? = null

    // Get a reference to the ViewModel scoped to this Fragment
    private val viewModel: LoginViewModel by activityViewModels()

    private lateinit var navController: NavController

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

/*        navController = findNavController()

        // If the user presses the back button, bring them back to the home screen.
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            navController.popBackStack(R.id.login_fragment, false)
        }*/

        // Observe the authentication state so we can know if the user has logged in successfully.
        // If the user has logged in successfully, bring them back to the login screen with current user details.
        // If the user did not log in successfully, bring them back to the login screen to let them login.
        viewModel.authenticationState.observe(viewLifecycleOwner, Observer { authenticationState ->
            when (authenticationState) {
                LoginViewModel.AuthenticationState.AUTHENTICATED -> {
                    cachedCredentials = viewModel.getCredentials()
                    cachedUserProfile = viewModel.getUserProfile()
                    //showSnackBar("Already authenticated")
                    updateUI()
                    showUserProfile()
                }
                else -> Log.e(
                    "LoginFragment",
                    "Authentication state that doesn't require any UI change $authenticationState"
                )
            }
        })

        // Set up the account object with the Auth0 application details
        account = Auth0(
            getString(R.string.com_auth0_client_id),
            getString(R.string.com_auth0_domain)
        )

        fragmentLoginBinding.buttonLogin.setOnClickListener { loginWithBrowser() }
        fragmentLoginBinding.buttonLogout.setOnClickListener { logout() }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_login, container, false)

        _fragmentLoginBinding = FragmentLoginBinding.inflate(inflater, container, false)
        return fragmentLoginBinding.root
    }

    private fun updateUI() {
        fragmentLoginBinding.buttonLogout.isEnabled = cachedCredentials != null
        fragmentLoginBinding.metadataPanel.isVisible = cachedCredentials != null
        fragmentLoginBinding.buttonLogin.isEnabled = cachedCredentials == null
        fragmentLoginBinding.userProfile.isVisible = cachedCredentials != null

        fragmentLoginBinding.userProfile.text =
            "Name: ${cachedUserProfile?.name ?: ""}\n" +
                    "ID: ${cachedUserProfile?.getId() ?: ""}\n" +
                    "Email: ${cachedUserProfile?.email ?: ""}"
    }

    private fun loginWithBrowser() {
        // Setup the WebAuthProvider, using the custom scheme and scope.
        WebAuthProvider.login(account)
            .withScheme(getString(R.string.com_auth0_scheme))
            .withScope(getString(R.string.com_auth0_scope))
            .withAudience(getString(R.string.com_auth0_audience))

            // Launch the authentication passing the callback where the results will be received
            .start(requireContext(), object : Callback<Credentials, AuthenticationException> {
                override fun onFailure(exception: AuthenticationException) {
                    showSnackBar("Failure: ${exception.getCode()}")
                }

                override fun onSuccess(credentials: Credentials) {
                    cachedCredentials = credentials
                    viewModel.setCredentials(cachedCredentials)
                    viewModel.setAuthenticationState(LoginViewModel.AuthenticationState.AUTHENTICATED)
                    showSnackBar("Success: ${credentials.accessToken}")
                    updateUI()
                    showUserProfile()
                }
            })
    }

    private fun logout() {
        WebAuthProvider.logout(account)
            .withScheme(getString(R.string.com_auth0_scheme))
            .start(requireContext(), object : Callback<Void?, AuthenticationException> {
                override fun onSuccess(payload: Void?) {
                    // The user has been logged out!
                    cachedCredentials = null
                    cachedUserProfile = null
                    viewModel.setCredentials(cachedCredentials)
                    viewModel.setUserProfile(cachedUserProfile)
                    viewModel.setAuthenticationState(LoginViewModel.AuthenticationState.UNAUTHENTICATED)
                    updateUI()
                }

                override fun onFailure(exception: AuthenticationException) {
                    viewModel.setAuthenticationState(LoginViewModel.AuthenticationState.INVALID_AUTHENTICATION)
                    updateUI()
                    showSnackBar("Failure: ${exception.getCode()}")
                }
            })
    }

    private fun showUserProfile() {
        val client = AuthenticationAPIClient(account)

        // Use the access token to call userInfo endpoint.
        // In this sample, we can assume cachedCredentials has been initialized by this point.
        client.userInfo(cachedCredentials!!.accessToken!!)
            .start(object : Callback<UserProfile, AuthenticationException> {
                override fun onFailure(exception: AuthenticationException) {
                    showSnackBar("Failure: ${exception.getCode()}")
                }

                override fun onSuccess(profile: UserProfile) {
                    cachedUserProfile = profile;
                    updateUI()
                }
            })
    }

    private fun showSnackBar(text: String) {
        Snackbar.make(
            fragmentLoginBinding.root,
            text,
            Snackbar.LENGTH_LONG
        ).show()
    }
}