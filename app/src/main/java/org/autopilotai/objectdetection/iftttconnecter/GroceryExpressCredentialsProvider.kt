package org.autopilotai.objectdetection.iftttconnecter

import com.ifttt.connect.ui.CredentialsProvider

class GroceryExpressCredentialsProvider(private val emailPreferencesHelper: EmailPreferencesHelper):
    CredentialsProvider {

    //override fun getUserToken() = ApiHelper.getUserToken(emailPreferencesHelper.getEmail())
    override fun getUserToken() = "m_EqS0kG01KAkJzWLm57W08DoUeU80XFOsEhs8JXm-cg6sIX"

    override fun getOAuthCode() = emailPreferencesHelper.getEmail()
}
