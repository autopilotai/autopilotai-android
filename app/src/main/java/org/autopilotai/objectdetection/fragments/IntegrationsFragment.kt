package org.autopilotai.objectdetection.fragments

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ifttt.connect.ui.ConnectButton
import com.ifttt.connect.ui.ConnectResult
import com.ifttt.connect.ui.CredentialsProvider
import com.ifttt.location.ConnectLocation
import com.squareup.picasso.Picasso
import org.autopilotai.objectdetection.LoginViewModel
import org.autopilotai.objectdetection.R
import org.autopilotai.objectdetection.databinding.FragmentIntegrationsBinding
import org.autopilotai.objectdetection.iftttconnecter.ApiHelper.REDIRECT_URI
import org.autopilotai.objectdetection.iftttconnecter.AutopilotAIApiHelper
import org.autopilotai.objectdetection.iftttconnecter.EmailPreferencesHelper
import org.autopilotai.objectdetection.iftttconnecter.FeatureView
import org.autopilotai.objectdetection.iftttconnecter.LocationForegroundService
import org.autopilotai.objectdetection.iftttconnecter.UiHelper.allPermissionsGranted
import org.autopilotai.objectdetection.iftttconnecter.UiHelper.appSettingsIntent
import java.util.*
import org.autopilotai.objectdetection.iftttconnecter.OnLabelClickListener
import org.autopilotai.objectdetection.iftttconnecter.LabelListAdapter
import org.autopilotai.objectdetection.iftttconnecter.LabelModel
import org.json.JSONArray

/**
 * A simple [Fragment] subclass.
 * Use the [IntegrationsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class IntegrationsFragment : Fragment() {

    private val TAG = "IntegrationsFragment"

    private lateinit var listView: ListView

    private var _fragmentIntegrationsBinding: FragmentIntegrationsBinding? = null
    private val fragmentIntegrationsBinding
        get() = _fragmentIntegrationsBinding!!

    /////////////////LABEL FORM
    private lateinit var addLabel: Button
    private lateinit var updateLabel: Button
    private lateinit var labelName: EditText
    private lateinit var labelList: RecyclerView
    /**
     * The adapter which we have prepared.
     */
    private lateinit var mLabelListAdapter: LabelListAdapter

    /**
     * To hold the reference to the items to be updated as a stack.
     * We can just remove and get the item with [Stack] in one shot.
     */
    private var modelToBeUpdated: Stack<LabelModel> = Stack()
    /**
     * The listener which we have defined in [OnLabelClickListener]. Will be added to the adapter
     * which constructing the adapter
     */
    private val mOnLabelClickListener = object : OnLabelClickListener {
        override fun onUpdate(position: Int, model: LabelModel) {
            // we want to update
            modelToBeUpdated.add(model)

            // set the value of the clicked item in the edit text
            labelName.setText(model.name)
        }

        override fun onDelete(model: LabelModel) {
            mLabelListAdapter.removeLabel(model)
        }
    }

    /////////////////IFTTT
    private lateinit var emailPreferencesHelper: EmailPreferencesHelper
    private lateinit var credentialsProvider: CredentialsProvider
    private lateinit var connectionId: String

    private lateinit var connectButton: ConnectButton
    private lateinit var toolbar: Toolbar
    private lateinit var features: LinearLayout

    // Get a reference to the ViewModel scoped to this Fragment
    private val viewModel: LoginViewModel by activityViewModels()

    // User preference on skip configuration flag from the menu.
    private var skipConnectionConfiguration: Boolean = false

    private val locationStatusCallback = object : ConnectLocation.LocationStatusCallback {
        override fun onRequestLocationPermission() {
            val permissionsToCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
            } else {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            if (!permissionsToCheck.allPermissionsGranted(requireActivity().applicationContext)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    /*
                    For Android Q and above, redirect users to settings and grant "Allow all the time"
                    location access, so that the app can get background location access.
                     */
                    AlertDialog.Builder(requireActivity().applicationContext).setMessage(R.string.background_location_permission_request)
                        .setPositiveButton(R.string.ok) { _, _ ->
                            startActivity(requireActivity().appSettingsIntent())
                        }.show()
                } else {
                    requestPermissions(permissionsToCheck, 0)
                }
            }
        }

        override fun onLocationStatusUpdated(activated: Boolean) {
            if (activated) {
                Toast.makeText(requireActivity().applicationContext, R.string.geofences_activated, Toast.LENGTH_SHORT).show()
                LocationForegroundService.startForegroundService(requireActivity().applicationContext)
            } else {
                Toast.makeText(requireActivity().applicationContext, R.string.geofences_deactivated, Toast.LENGTH_SHORT).show()
                LocationForegroundService.stopForegroundService(requireActivity().applicationContext)
            }
        }
    }

    private val fetchCompleteListener = ConnectButton.OnFetchConnectionListener {
        fragmentIntegrationsBinding.connectionTitle.text = it.name
        features.removeAllViews()
        it.features.forEach {
            val featureView = FeatureView(requireActivity().applicationContext).apply {
                text = it.title
                Picasso.get().load(it.iconUrl).into(this)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    val margin = resources.getDimensionPixelSize(R.dimen.feature_margin_horizontal)
                    setMargins(0, 0, 0, margin)
                }
            }

            features.addView(featureView)
        }
    }

    /*
    Demonstrate setting up a connection.
     */
    private fun setupForConnection() {
        val suggestedEmail = viewModel.getUserProfile()?.email ?: IntegrationsFragment.EMAIL
        val configurationBuilder = ConnectButton.Configuration.newBuilder(suggestedEmail, REDIRECT_URI)
            .withConnectionId(connectionId)
            .withCredentialProvider(credentialsProvider)
            .setOnFetchCompleteListener(fetchCompleteListener)

        if (skipConnectionConfiguration) {
            configurationBuilder.skipConnectionConfiguration()
        }
        connectButton.setOnDarkBackground(true)
        connectButton.setup(configurationBuilder.build())
    }

    /*
    Demonstrate setting up a connection using Location service with the connect-location module: in addition to calling
    ConnectButton#setup, you should also call ConnectLocation#setUpWithConnectButton to set up the ConnectLocation
    instance to listen to ConnectButton's state changes, which can be used to configure ConnectLocation.
    */
    private fun setupForLocationConnection() {
        setupForConnection()
        ConnectLocation.getInstance().setUpWithConnectButton(connectButton, locationStatusCallback)
    }
//
//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        menuInflater.inflate(R.menu.menu, menu)
//        menu.findItem(R.id.skip_config).isChecked = skipConnectionConfiguration
//
//        return super.onCreateOptionsMenu(menu)
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        val updateConnectionAction: () -> Unit = {
//            if (connectionId == IntegrationsFragment.CONNECTION_ID_LOCATION) {
//                setupForLocationConnection()
//            } else {
//                setupForConnection()
//            }
//        }
//
//        if (item.itemId == R.id.set_email) {
//            promptLogin {
//                updateConnectionAction()
//            }
//            return true
//        } else if (item.itemId == R.id.skip_config) {
//            item.isChecked = !item.isChecked
//            skipConnectionConfiguration = item.isChecked
//            updateConnectionAction()
//            return true
//        }
//
//        return super.onOptionsItemSelected(item)
//    }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//
//        // Check if the location permission wasn't granted.
//        if (grantResults.firstOrNull { it != PackageManager.PERMISSION_GRANTED } != null) {
//            *//*
//            Possibly show a message or any other error/warning indication for the connection not being able to work as expected
//             *//*
//        } else {
//            ConnectLocation.getInstance().activate(requireActivity().applicationContext,
//                IntegrationsFragment.CONNECTION_ID_LOCATION, locationStatusCallback)
//        }
//    }
//
//    /*
//    For development and testing purpose: this dialog simulates a login process, where the user enters their
//    email, and the app tries to fetch the IFTTT user token for the user. In the case where the user token
//    is empty, we treat it as the user have never connected the service to IFTTT before.
//    */
//    private fun promptLogin(onComplete: () -> Unit) {
//        val emailView =
//            LayoutInflater.from(requireActivity().applicationContext).inflate(
//                R.layout.view_email,
//                container,
//                false
//            ) as TextInputLayout
//        emailView.editText!!.setText(emailPreferencesHelper.getEmail())
//
//        AlertDialog.Builder(requireActivity().applicationContext)
//            .setView(emailView)
//            .setTitle(R.string.email_title)
//            .setPositiveButton(R.string.login) { _, _ ->
//                val newEmail = emailView.editText!!.text.toString()
//                emailPreferencesHelper.setEmail(newEmail)
//                onComplete()
//            }.setNegativeButton(R.string.logout) { _, _ ->
//                emailPreferencesHelper.clear()
//                onComplete()
//            }.setOnCancelListener {
//                onComplete()
//            }
//            .show()
//    }
    ///////////////end IFTTT

    /*
    Display Image Labels per connection
    */
    private fun displayImageLabels() {
        try {
            if (connectButton.isEnabled) {
                mLabelListAdapter.clearLabels()

                val connInfo = AutopilotAIApiHelper.ConnectionInfo(
                    user_id = viewModel.getCredentials()?.user?.getId(),
                    connection_id = connectionId,
                    image_labels = ""
                )
                val userAuthToken = "Bearer " + viewModel.getCredentials()?.accessToken
                val response = JSONArray(AutopilotAIApiHelper.getImageLabels(userAuthToken, connInfo) ?: "[]")

                (0 until response.length()).forEach {
                    // prepare id on incremental basis
                    val id = mLabelListAdapter.getNextItemId()
                    val image = response[it].toString()

                    // prepare model for use
                    val model = LabelModel(id, image)

                    // add model to the adapter
                    mLabelListAdapter.addLabel(model)
                }
            }
        } catch (exc: Exception) {
            Log.e(TAG, "Getting Image Labels Failed", exc)
        }
    }

    /*
    Update Image Labels per connection
    */
    private fun updateImageLabels(): String {
        try {
            if (connectButton.isEnabled) {
                val imageLabels = mLabelListAdapter.getList()

                val connInfo = AutopilotAIApiHelper.ConnectionInfo(
                    user_id = viewModel.getCredentials()?.user?.getId(),
                    connection_id = connectionId,
                    image_labels = imageLabels.toString()
                )
                val userAuthToken = "Bearer " + viewModel.getCredentials()?.accessToken
                val response = AutopilotAIApiHelper.setImageLabels(userAuthToken, connInfo)
                return response.toString()
            }
        } catch (exc: Exception) {
            Log.e(TAG, "Setting Image Labels Failed", exc)
        }
        return "Nothing was sent to IFTTT"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

/*        if (viewModel.getAuthenticationState() == LoginViewModel.AuthenticationState.UNAUTHENTICATED) {
            Navigation.findNavController(requireActivity(), R.id.fragment_container)
                .navigate(IntegrationsFragmentDirections.actionIntegrationsFragmentToLoginFragment())
        }*/

/*        val navController = findNavController()
        viewModel.authenticationState.observe(viewLifecycleOwner, Observer { authenticationState ->
            when (authenticationState) {
                LoginViewModel.AuthenticationState.AUTHENTICATED -> Log.i(TAG, "Authenticated")
                // If the user is not logged in, they should not be able to set any preferences,
                // so navigate them to the login fragment
                LoginViewModel.AuthenticationState.UNAUTHENTICATED -> navController.navigate(
                    R.id.login_fragment
                )
                else -> Log.e(
                    TAG, "New $authenticationState state that doesn't require any UI change"
                )
            }
        })*/

        val policy = StrictMode.ThreadPolicy.Builder()
            .permitAll().build()
        StrictMode.setThreadPolicy(policy)

        if (viewModel.getUserToken() == null) {
            val userInfo = AutopilotAIApiHelper.UserInfo(
                user_id = viewModel.getCredentials()?.user?.getId(),
            )
            val userAuthToken = "Bearer " + viewModel.getCredentials()?.accessToken
            val userIFTTTToken = AutopilotAIApiHelper.getIFTTTUserToken(userAuthToken, userInfo)
            viewModel.setUserToken(userIFTTTToken)
        }

        connectButton = fragmentIntegrationsBinding.connectButton
        features = fragmentIntegrationsBinding.features
        skipConnectionConfiguration = false;
        credentialsProvider = object : CredentialsProvider {
            override fun getOAuthCode(): String? {
                // Your user's OAuth code, this will be used to authenticate the user to IFTTT.
                return viewModel.getUserProfile()?.email
            }

            override fun getUserToken(): String? {
                return viewModel.getUserToken()
            }
        }

        listView = fragmentIntegrationsBinding.connectionsList
        val connections: Array<String> = resources.getStringArray(R.array.connections)
        val connection_ids: Array<String> = resources.getStringArray(R.array.connection_ids)

        connectionId = connection_ids.first();
        setupForConnection();

        val adapter = ArrayAdapter(
            requireActivity().applicationContext,
            android.R.layout.simple_list_item_1,
            connections
        )
        listView.adapter = adapter

        listView.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, position, id ->
                val selectedItem = adapterView.getItemAtPosition(position) as String
                val itemIdAtPos = adapterView.getItemIdAtPosition(position)
                connectionId = connection_ids[itemIdAtPos.toInt()];

                //Toast.makeText(requireActivity().applicationContext,"click item $selectedItem its position $itemIdAtPos",Toast.LENGTH_SHORT).show()
                setupForConnection();

                displayImageLabels();
            }

        // initialize the recycler view
        labelList = fragmentIntegrationsBinding.labelListRecyclerView
        labelList.layoutManager = LinearLayoutManager(requireActivity().applicationContext)
        labelList.setHasFixedSize(true)

        mLabelListAdapter = LabelListAdapter(requireActivity().applicationContext, mOnLabelClickListener)
        labelList.adapter = mLabelListAdapter

        displayImageLabels();

        labelName = fragmentIntegrationsBinding.labelName

        updateLabel = fragmentIntegrationsBinding.updateLabel
        updateLabel.setOnClickListener {
            val msg = updateImageLabels()
            Toast.makeText(requireActivity().applicationContext,msg,Toast.LENGTH_SHORT).show()
        }

        addLabel = fragmentIntegrationsBinding.addLabel
        addLabel.setOnClickListener {

            val name = labelName.text.toString()

            if (name.isNotBlank() && modelToBeUpdated.isEmpty()) {

                // prepare id on incremental basis
                val id = mLabelListAdapter.getNextItemId()

                // prepare model for use
                val model = LabelModel(id, name)

                // add model to the adapter
                mLabelListAdapter.addLabel(model)

                // reset the input
                labelName.setText("")

                Toast.makeText(requireActivity().applicationContext,"Added label",Toast.LENGTH_SHORT).show()
            } else if (modelToBeUpdated.isNotEmpty()) {
                val model = modelToBeUpdated.pop()
                model.name = name
                mLabelListAdapter.updateLabel(model)

                // reset the input
                labelName.setText("")

                Toast.makeText(requireActivity().applicationContext,"Updated label",Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _fragmentIntegrationsBinding = FragmentIntegrationsBinding.inflate(inflater, container, false)
        return fragmentIntegrationsBinding.root
    }

    override fun onResume() {
        super.onResume()
        requireActivity().intent?.let {
            connectButton.setConnectResult(ConnectResult.fromIntent(it))
        }
    }

    override fun onStart() {
        super.onStart()
        requireActivity().intent?.let {
            connectButton.setConnectResult(ConnectResult.fromIntent(it))
        }
    }

    companion object {
        private const val EMAIL = "user@email.com"
        private const val KEY_CONNECTION_ID = "key_connection_id"
        private const val KEY_SKIP_CONFIG = "key_skip_config"
    }
}