package com.example.randommemories.ui.main

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.view.animation.*
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat.finishAffinity
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import com.example.randommemories.R
import com.example.randommemories.helpers.OnSwipeTouchListener
import kotlin.system.exitProcess

class MenuFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_menu, container, false)

        rootView.setOnTouchListener(
            OnSwipeTouchListener(
                requireContext(),
                onSwipeRight = { removeFragment() })
        )

        return rootView
    }

    @SuppressLint("CutPasteId", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.findViewById<TextView>(R.id.logo)?.visibility = View.GONE
        activity?.findViewById<Button>(R.id.menu_button)?.visibility = View.GONE


        view.findViewById<Button>(R.id.backButton).setOnClickListener {
            removeFragment()
        }

        view.findViewById<Button>(R.id.logout_button).setOnClickListener{
            showLogoutDialog()
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
        menuBackgroundLayout.setOnTouchListener(
            OnSwipeTouchListener(
                requireContext(),
                onSwipeRight = { removeFragment() },
                onSimpleTouch = { removeFragment() })
        )


        val editButton = view.findViewById<Button>(R.id.edit_button)
        val contentTextView = view.findViewById<TextView>(R.id.contentTextView)
        val contentEditText = view.findViewById<EditText>(R.id.contentEditText)

        // Set initial text for both TextView and EditText
        val initialText = requireContext().getString(R.string.address)
        contentTextView.text = initialText
        contentEditText.setText(initialText)

        editButton.setOnClickListener {
            val isEditing = contentEditText.visibility == View.VISIBLE

            if (isEditing) {
                // Switch from edit mode to view mode
                contentTextView.text = contentEditText.text.toString()
                contentEditText.visibility = View.INVISIBLE
                contentTextView.visibility = View.VISIBLE
                editButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_edit, 0)
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(contentEditText.windowToken, 0)
            } else {
                // Switch from view mode to edit mode
                contentEditText.setText(contentTextView.text.toString())
                contentEditText.visibility = View.VISIBLE
                contentTextView.visibility = View.INVISIBLE
                editButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_checkcircle, 0)
                contentEditText.requestFocus()
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(contentEditText, InputMethodManager.SHOW_IMPLICIT)
            }
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

    private fun showLogoutDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.logout_dialog, null)
        val builder = AlertDialog.Builder(requireContext(), R.style.squareDialog)
            .setView(dialogView)

        val dialog = builder.create()
        dialogView.findViewById<Button>(R.id.exit_button).setOnClickListener {
            dialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.accept_snooze_button).setOnClickListener {
            dialog.dismiss()
            navigateToHomeFragment()
        }

        dialog.show()
    }


    private fun navigateToHomeFragment() {
        val homeFragment = HomeFragment.newInstance(false)

        val fragmentManager = requireActivity().supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, homeFragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun closeFragment() {
        val fragmentManager = requireActivity().supportFragmentManager
        fragmentManager.popBackStack("home", POP_BACK_STACK_INCLUSIVE)
    }

    // works without fade
//    private fun removeFragment() {
//        val thisFrag = this
//
//        val menuContentLayout = activity?.findViewById<LinearLayout>(R.id.menu_content_layout)
//
//        val slideOutAnimation = TranslateAnimation(
//            Animation.RELATIVE_TO_PARENT, 0.0f,
//            Animation.RELATIVE_TO_PARENT, 1.0f,
//            Animation.RELATIVE_TO_PARENT, 0.0f,
//            Animation.RELATIVE_TO_PARENT, 0.0f
//        )
//        slideOutAnimation.duration = 100
//        slideOutAnimation.interpolator = DecelerateInterpolator()
//        menuContentLayout?.startAnimation(slideOutAnimation)
//
//        slideOutAnimation.setAnimationListener(object : Animation.AnimationListener {
//            override fun onAnimationStart(animation: Animation?) {}
//
//            override fun onAnimationEnd(animation: Animation?) {
//                val fragmentManager = requireActivity().supportFragmentManager
//                fragmentManager.beginTransaction()
//                    .remove(thisFrag)
//                    .commit()
//            }
//
//            override fun onAnimationRepeat(animation: Animation?) {}
//        })
//    }

    private fun removeFragment() {
        val thisFrag = this

        val menuContentLayout = activity?.findViewById<LinearLayout>(R.id.menu_content_layout)
        val menuDimmedBackground = activity?.findViewById<LinearLayout>(R.id.menu_dimmed_background)

        val slideOutAnimation = TranslateAnimation(
            Animation.RELATIVE_TO_PARENT, 0.0f,
            Animation.RELATIVE_TO_PARENT, 1.0f,
            Animation.RELATIVE_TO_PARENT, 0.0f,
            Animation.RELATIVE_TO_PARENT, 0.0f
        )
        slideOutAnimation.duration = 100
        slideOutAnimation.interpolator = DecelerateInterpolator()
        slideOutAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                menuContentLayout?.visibility = View.GONE

                val fadeOutAnimation = AlphaAnimation(0.7f, 0.0f)
                fadeOutAnimation.duration = 100
                fadeOutAnimation.interpolator = AccelerateInterpolator()
                fadeOutAnimation.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}

                    override fun onAnimationEnd(animation: Animation?) {
                        val fragmentManager = requireActivity().supportFragmentManager
                        fragmentManager.beginTransaction()
                            .remove(thisFrag)
                            .commit()
                    }

                    override fun onAnimationRepeat(animation: Animation?) {}
                })
                menuDimmedBackground?.startAnimation(fadeOutAnimation)
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })
        menuContentLayout?.startAnimation(slideOutAnimation)
    }
}