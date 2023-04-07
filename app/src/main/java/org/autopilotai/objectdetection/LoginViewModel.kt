/*
 * Copyright 2023 AutoPilot AI. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.autopilotai.objectdetection

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.auth0.android.result.Credentials
import com.auth0.android.result.UserProfile

class LoginViewModel : ViewModel() {
    private val _authenticationState = MutableLiveData(AuthenticationState.UNAUTHENTICATED)
    val authenticationState get() = _authenticationState

    private val _credentials: MutableLiveData<Credentials?> = MutableLiveData()
    val credentials get() = _credentials

    private val _userProfile: MutableLiveData<UserProfile?> = MutableLiveData()
    val userProfile get() = _userProfile

    private val _userToken: MutableLiveData<String?> = MutableLiveData()
    val userToken get() = _userToken

    fun setAuthenticationState(state: AuthenticationState) {
        _authenticationState.value = state
    }

    fun getAuthenticationState() = authenticationState.value

    enum class AuthenticationState {
        AUTHENTICATED, UNAUTHENTICATED, INVALID_AUTHENTICATION
    }

    fun setCredentials(creds: Credentials?) {
        _credentials.value = creds
    }

    fun getCredentials() = credentials.value

    fun setUserProfile(userinfo: UserProfile?) {
        _userProfile.value = userinfo
    }

    fun getUserProfile() = userProfile.value

    fun setUserToken(userdata: String?) {
        _userToken.value = userdata
    }

    fun getUserToken() = userToken.value
}
