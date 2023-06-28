package com.example.randommemories.ui.main

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.randommemories.R


private const val ARG_IS_ACTIVE_DIARY = "param1"
private const val KEY_IS_VIDEO_PLAYING = "is_playing_key"

class HomeFragment : Fragment() {

    private var activeDiary: Boolean? = null
    private var playbackPosition = 0
    private var isVideoPlaying = false
    private var menuButton: Button? = null
    private var logo: TextView? = null
    private lateinit var video: VideoView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            activeDiary = it.getBoolean(ARG_IS_ACTIVE_DIARY)
        }
    }

    override fun onPause() {
        super.onPause()
        pauseVideoPlayback()
    }

    override fun onResume() {
        super.onResume()
        resumeVideoPlayback()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_IS_VIDEO_PLAYING, isVideoPlaying)
    }

    @SuppressLint("ResourceType", "MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    @SuppressLint("ResourceType")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {}

        logo = activity?.findViewById<TextView>(R.id.logo)?.apply { visibility = View.VISIBLE }
        menuButton =
            activity?.findViewById<Button>(R.id.menu_button)?.apply {
                visibility = View.VISIBLE
                background.clearColorFilter()
            }
        menuButton?.setOnClickListener {
            navigateToMenuFragment()
        }

        val startButton = view.findViewById<Button>(R.id.start_button)
        startButton.setOnClickListener {
            showDialog()
        }

        if (activeDiary == true) {
            startButton.visibility = View.GONE
        }

        val text = view.findViewById<TextView>(R.id.text)
        if (activeDiary == true) {
            text.setText(R.string.home_fragment_no_moment)
        } else {
            text.setText(R.string.home_fragment_no_diary)
        }

        video = view.findViewById<VideoView>(R.id.video)
        if (savedInstanceState == null) {
            video.setVideoURI(Uri.parse("android.resource://" + context?.packageName + "/" + R.raw.home_screen))
            video.start()
            video.setOnCompletionListener {
                video.seekTo(0)
                video.start()
            }
            video.setOnPreparedListener { mp ->
                mp.setOnInfoListener { _, what, _ ->
                    if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                        view.findViewById<ImageView>(R.id.first_frame).visibility = View.GONE
                        return@setOnInfoListener true
                    }
                    return@setOnInfoListener false
                }
            }
        } else {
            // Restore the playback position and state from the saved instance state
            isVideoPlaying = savedInstanceState.getBoolean(KEY_IS_VIDEO_PLAYING, false)

            if (isVideoPlaying) {
                resumeVideoPlayback()
            } else {
                pauseVideoPlayback()
            }
        }
    }

    private fun pauseVideoPlayback() {
        video.pause()
        playbackPosition = video.currentPosition
        isVideoPlaying = false
    }

    private fun resumeVideoPlayback() {
        video.seekTo(0)
        video.start()
        isVideoPlaying = true
    }

    private fun showDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.start_dialog, null)
        val builder = AlertDialog.Builder(requireContext(), R.style.squareDialog)
            .setView(dialogView)

        val dialog = builder.create()
        dialogView.findViewById<Button>(R.id.dialog_button).setOnClickListener {
            navigateToWriteFragment()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun navigateToWriteFragment() {
        val writeFragment = WriteFragment()

        val fragmentManager = requireActivity().supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, writeFragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun navigateToMenuFragment() {
        val menuFragment = MenuFragment()

        val fragmentManager = requireActivity().supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, menuFragment)
        transaction.addToBackStack("home")
        transaction.commit()
    }

    companion object {
        @JvmStatic
        fun newInstance(activeDiary: Boolean) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_IS_ACTIVE_DIARY, activeDiary)
                }
            }
    }
}