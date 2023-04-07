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

package org.autopilotai.objectdetection.fragments

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import org.autopilotai.objectdetection.LoginViewModel
import org.autopilotai.objectdetection.MainViewModel
import org.autopilotai.objectdetection.R
import org.autopilotai.objectdetection.databinding.FragmentSettingBinding
import androidx.lifecycle.Observer


class SettingFragment : DialogFragment() {

    private val TAG = "SettingsFragment"

    private var _fragmentSettingBinding: FragmentSettingBinding? = null
    private val fragmentSettingBinding
        get() = _fragmentSettingBinding!!

    private var numThreads = 2
    private val viewModel: MainViewModel by activityViewModels()

    private val viewModel2: LoginViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _fragmentSettingBinding = FragmentSettingBinding.inflate(
            inflater,
            container,
            false
        )
        return fragmentSettingBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

/*        val navController = findNavController()
        viewModel2.authenticationState.observe(viewLifecycleOwner, Observer { authenticationState ->
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

        viewModel.getNumThreads()?.let {
            numThreads = it
            updateDialogUi()
        }

        initDialogControls()

        fragmentSettingBinding.btnConfirm.setOnClickListener {
            viewModel.configModel(numThreads)
            dismiss()
        }
        fragmentSettingBinding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun initDialogControls() {
        // When clicked, decrease the number of threads used for classification
        fragmentSettingBinding.threadsMinus.setOnClickListener {
            if (numThreads > 1) {
                numThreads--
                updateDialogUi()
            }
        }

        // When clicked, increase the number of threads used for classification
        fragmentSettingBinding.threadsPlus.setOnClickListener {
            if (numThreads < 4) {
                numThreads++
                updateDialogUi()
            }
        }
    }

    //  Update the values displayed in the dialog.
    private fun updateDialogUi() {
        fragmentSettingBinding.threadsValue.text =
            numThreads.toString()
    }
}
