package org.autopilotai.objectdetection.iftttconnecter

import android.app.Application
import org.autopilotai.objectdetection.iftttconnecter.NotificationsHelper.sendNotification
import com.ifttt.location.ConnectLocation

class GroceryExpressApplication : Application() {

    /**
     * Initialize Location module here, so that it can set up polling and get the most updated location field values
     */
    override fun onCreate() {
        super.onCreate()
        ConnectLocation.init(this, GroceryExpressCredentialsProvider(EmailPreferencesHelper(this)))
        with(ConnectLocation.getInstance()) {
            setLoggingEnabled(true)
            setLocationEventListener { type, data ->
                sendNotification("$type $data")
            }
        }
    }

}
