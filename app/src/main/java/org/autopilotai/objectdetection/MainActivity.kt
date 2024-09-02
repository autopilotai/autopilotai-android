/*
 * Copyright 2023 AutoPilot AI. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.autopilotai.objectdetection

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.NavHostFragment
import org.autopilotai.objectdetection.databinding.ActivityMainBinding
import org.autopilotai.objectdetection.fragments.*

const val KEY_EVENT_ACTION = "key_event_action"
const val KEY_EVENT_EXTRA = "key_event_extra"
private const val IMMERSIVE_FLAG_TIMEOUT = 500L

/**
 * Main entry point into our app. This app follows the single-activity pattern, and all
 * functionality is implemented in the form of fragments.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var activityMainBinding: ActivityMainBinding

    lateinit var bottomNav : BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        val navController = navHostFragment.navController
        bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.home -> {
                    navController.navigate(R.id.camera_fragment)
                    true
                }
                R.id.training -> {
                    navController.navigate(R.id.training_fragment)
                    true
                }
                R.id.integrations -> {
                    navController.navigate(R.id.integrations_fragment)
                    true
                }
                R.id.chat -> {
                    navController.navigate(R.id.chat_fragment)
                    true
                }
//                R.id.settings -> {
//                    navController.navigate(R.id.settings_fragment)
//                    true
//                }
                R.id.login -> {
                    navController.navigate(R.id.login_fragment)
                    true
                }
                else -> {
                    navController.navigate(R.id.camera_fragment)
                    true
                }
            }
        }

        /*//////////////////IFTTT
        emailPreferencesHelper = EmailPreferencesHelper(this)
        credentialsProvider = GroceryExpressCredentialsProvider(emailPreferencesHelper);

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        title = null

        connectButton = findViewById(R.id.connect_button)
        features = findViewById(R.id.features)

        skipConnectionConfiguration = savedInstanceState?.getBoolean(MainActivity.KEY_SKIP_CONFIG) ?: false
        if (savedInstanceState?.containsKey(MainActivity.KEY_CONNECTION_ID) == true) {
            connectionId = savedInstanceState.getString(MainActivity.KEY_CONNECTION_ID)!!
            if (connectionId == MainActivity.CONNECTION_ID_LOCATION) {
                setupForLocationConnection()
            } else {
                setupForConnection()
            }
            return
        }

        if (TextUtils.isEmpty(emailPreferencesHelper.getEmail())) {
            promptLogin {
                promptConnectionSelection()
            }
        } else {
            promptConnectionSelection()
        }
        /////////////////End IFTTT*/
    }

    override fun onBackPressed() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            // Workaround for Android Q memory leak issue in IRequestFinishCallback$Stub.
            // (https://issuetracker.google.com/issues/139738913)
            finishAfterTransition()
        } else {
            super.onBackPressed()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

//    override fun onResume() {
//        super.onResume()
//        // Before setting full screen flags, we must wait a bit to let UI settle; otherwise, we may
//        // be trying to set app to immersive mode before it's ready and the flags do not stick
//        activityMainBinding.fragmentContainer.postDelayed({
//            hideSystemUI()
//        }, IMMERSIVE_FLAG_TIMEOUT)
//    }

    /** When key down event is triggered, relay it via local broadcast so fragments can handle it */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                val intent = Intent(KEY_EVENT_ACTION).apply { putExtra(KEY_EVENT_EXTRA, keyCode) }
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}
