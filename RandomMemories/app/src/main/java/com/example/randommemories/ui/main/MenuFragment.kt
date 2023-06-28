package com.example.randommemories.ui.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.view.animation.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import com.example.randommemories.R

class MenuFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_menu, container, false)
    }

    @SuppressLint("CutPasteId")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.findViewById<TextView>(R.id.logo)?.visibility = View.GONE
        activity?.findViewById<Button>(R.id.menu_button)?.visibility = View.GONE


        view.findViewById<Button>(R.id.backButton).setOnClickListener {
            closeFragment()
        }

        val menuContentLayout = view.findViewById<LinearLayout>(R.id.menu_content_layout)
        val slideInAnimation = TranslateAnimation(
            Animation.RELATIVE_TO_PARENT, 1.0f,
            Animation.RELATIVE_TO_PARENT, 0.0f,
            Animation.RELATIVE_TO_PARENT, 0.0f,
            Animation.RELATIVE_TO_PARENT, 0.0f
        )
        slideInAnimation.duration = 400
        slideInAnimation.interpolator = AccelerateDecelerateInterpolator()
        slideInAnimation.interpolator = DecelerateInterpolator()
        menuContentLayout.startAnimation(slideInAnimation)

        val menuBackgroundLayout = view.findViewById<LinearLayout>(R.id.menu_dimmed_background)
        val fadeAnimation = AlphaAnimation(0.0f, 1.0f)
        fadeAnimation.duration = 100 // Set the duration in milliseconds
        fadeAnimation.interpolator =
            AccelerateDecelerateInterpolator() // Set an interpolator for smoother animation
        menuBackgroundLayout.startAnimation(fadeAnimation)
        menuBackgroundLayout.setOnClickListener {
            closeFragment()
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.findViewById<TextView>(R.id.logo)?.visibility = View.GONE
        activity?.findViewById<Button>(R.id.menu_button)?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        activity?.findViewById<TextView>(R.id.logo)?.visibility = View.VISIBLE
        activity?.findViewById<Button>(R.id.menu_button)?.visibility = View.VISIBLE
    }

    private fun closeFragment() {
        val fragmentManager = requireActivity().supportFragmentManager
        fragmentManager.popBackStack("home", POP_BACK_STACK_INCLUSIVE)
    }
}