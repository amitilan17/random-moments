package com.example.randommemories.ui.main

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AlertDialog
import com.example.randommemories.R


private const val ARG_IS_ACTIVE_DIARY = "param1"

class HomeFragment : Fragment() {

    private var activeDiary: Boolean? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            activeDiary = it.getBoolean(ARG_IS_ACTIVE_DIARY)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        val video = view.findViewById<VideoView>(R.id.video)
        video.setVideoURI(Uri.parse("android.resource://" + context?.packageName + "/" + R.raw.home_screen))
        video.start()
        video.setOnCompletionListener {
            video.seekTo(1)
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