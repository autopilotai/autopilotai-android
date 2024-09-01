/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.autopilotai.objectdetection.fragments

import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.autopilotai.objectdetection.R
import org.autopilotai.objectdetection.databinding.FragmentGalleryBinding
import org.autopilotai.objectdetection.utils.MediaStoreFile
import org.autopilotai.objectdetection.utils.MediaStoreUtils
import org.autopilotai.objectdetection.utils.padWithDisplayCutout
import org.autopilotai.objectdetection.utils.showImmersive
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch

/** Fragment used to present the user with a gallery of photos taken */
class GalleryFragment internal constructor() : Fragment() {

    /** Android ViewBinding */
    private var _fragmentGalleryBinding: FragmentGalleryBinding? = null

    private val fragmentGalleryBinding get() = _fragmentGalleryBinding!!

    /** AndroidX navigation arguments */
    private val args: GalleryFragmentArgs by navArgs()

    private var mediaList: MutableList<MediaStoreFile> = mutableListOf()
    private var hasMediaItems = CompletableDeferred<Boolean>()

    /** Adapter class used to present a fragment containing one photo or video as a page */
    inner class MediaPagerAdapter(fm: FragmentManager,
                                  private var mediaList: MutableList<MediaStoreFile>) :
                                      FragmentStateAdapter(fm, lifecycle) {
        override fun getItemCount(): Int = mediaList.size
        override fun createFragment(position: Int): Fragment =
            PhotoFragment.create(mediaList[position])
        override fun getItemId(position: Int): Long {
            return mediaList[position].id
        }
        override fun containsItem(itemId: Long): Boolean {
            return null != mediaList.firstOrNull { it.id == itemId }
        }
        fun setMediaListAndNotify(mediaList: MutableList<MediaStoreFile>) {
            this.mediaList = mediaList
            notifyDataSetChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            // Get images this app has access to from MediaStore
            mediaList = MediaStoreUtils(requireContext()).getImages()
            (fragmentGalleryBinding.photoViewPager.adapter as MediaPagerAdapter)
                .setMediaListAndNotify(mediaList)
            hasMediaItems.complete(mediaList.isNotEmpty())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentGalleryBinding = FragmentGalleryBinding.inflate(inflater, container, false)
        return fragmentGalleryBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val messageET = view.findViewById<EditText>(R.id.messageET)

        lifecycleScope.launch {
            fragmentGalleryBinding.deleteButton.isEnabled = hasMediaItems.await()
            fragmentGalleryBinding.shareButton.isEnabled = hasMediaItems.await()
        }

        // Populate the ViewPager and implement a cache of two media items
        fragmentGalleryBinding.photoViewPager.apply {
            offscreenPageLimit = 2
            adapter = MediaPagerAdapter(childFragmentManager, mediaList)
        }

        // Make sure that the cutout "safe area" avoids the screen notch if any
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Use extension method to pad "inside" view containing UI using display cutout's bounds
            fragmentGalleryBinding.cutoutSafeArea.padWithDisplayCutout()
        }

        // Handle back button press
        fragmentGalleryBinding.backButton.setOnClickListener {
            Navigation.findNavController(requireActivity(), R.id.fragment_container)
                .navigateUp()
        }

        // Handle send button press
        fragmentGalleryBinding.shareButton.setOnClickListener {

            mediaList.getOrNull(fragmentGalleryBinding.photoViewPager.currentItem)
                ?.let { mediaStoreFile ->
                    // Retrieve the text from EditText
                    val message = messageET.text.toString()

                    // Retrieve the image resource ID from the mediaStoreFile
                    val imageResId = getImageResIdFromMediaStore(mediaStoreFile.uri)

                    val mediaFile = mediaStoreFile.file
//                    Create a sharing intent
//                    val intent = Intent().apply {
//                        // Infer media type from file extension
//                        val mediaType = MimeTypeMap.getSingleton()
//                            .getMimeTypeFromExtension(mediaFile.extension)
//                        // Set the appropriate intent extra, type, action and flags
//                        type = mediaType
//                        action = Intent.ACTION_SEND
//                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
//                        putExtra(Intent.EXTRA_STREAM, mediaStoreFile.uri)
//                    }

                    // Navigate to ChatFragment with the retrieved data
                    findNavController().navigate(GalleryFragmentDirections.actionGalleryToChat(imageResId, message))
                }
        }

        // Handle delete button press
        fragmentGalleryBinding.deleteButton.setOnClickListener {

            mediaList.getOrNull(fragmentGalleryBinding.photoViewPager.currentItem)
                ?.let { mediaStoreFile ->
                    val mediaFile = mediaStoreFile.file

                    AlertDialog.Builder(view.context, android.R.style.Theme_Material_Dialog)
                        .setTitle(getString(R.string.delete_title))
                        .setMessage(getString(R.string.delete_dialog))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.ok) { _, _ ->

                            // Delete current photo
                            mediaFile.delete()

                            // Send relevant broadcast to notify other apps of deletion
                            MediaScannerConnection.scanFile(
                                view.context, arrayOf(mediaFile.absolutePath), null, null
                            )

                            // Notify our view pager
                            mediaList.removeAt(fragmentGalleryBinding.photoViewPager.currentItem)
                            fragmentGalleryBinding.photoViewPager.adapter?.notifyDataSetChanged()

                            // If all photos have been deleted, return to camera
                            if (mediaList.isEmpty()) {
                                Navigation.findNavController(
                                    requireActivity(),
                                    R.id.fragment_container
                                ).navigateUp()
                            }

                        }

                        .setNegativeButton(android.R.string.cancel, null)
                        .create().showImmersive()
                }
        }
    }

    override fun onDestroyView() {
        _fragmentGalleryBinding = null
        super.onDestroyView()
    }

    private fun getImageResIdFromMediaStore(uri: Uri?): Int {
        if (uri == null) return 0

        val projection = arrayOf(MediaStore.Images.Media._ID)
        val cursor = requireContext().contentResolver.query(uri, projection, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                return it.getInt(idColumn)
            }
        }
        return 0
    }
}
