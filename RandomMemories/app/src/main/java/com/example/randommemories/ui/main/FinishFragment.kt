package com.example.randommemories.ui.main

import android.os.Bundle
import android.text.Editable
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import com.example.randommemories.R


/**
 * A simple [Fragment] subclass.
 * Use the [FinishFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FinishFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_finish, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner){}

        val back = view.findViewById<Button>(R.id.finish_exit_button)
        back.setOnClickListener {
//            requireActivity().finish() todo return if no demo restart button
//            navigateToHomeFragment()
            showSendToEmailDialog()
        }
    }

    private fun showSendToEmailDialog() {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.send_to_email_dialog, null)
        val builder = AlertDialog.Builder(requireContext(), R.style.squareDialog)
            .setView(dialogView)
        val dialog = builder.create()

        val emailEditText = dialogView.findViewById<EditText>(R.id.email_editText)

        dialogView.findViewById<Button>(R.id.exit_button).setOnClickListener {
            navigateToHomeFragment() // todo switch with dismiss for smoother animation?
            dialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.send_button).setOnClickListener {
            if (validateEmail(emailEditText.text)){
                // todo send to email server
                navigateToHomeFragment() // todo switch with dismiss for smoother animation?
                dialog.dismiss()
            }
        }

        dialog.show()

    }

    private fun validateEmail(text1: Editable): Boolean {
        return if (text1.isEmpty()) {
            Toast.makeText(requireActivity(), INVALID_EMAIL_TOAST, Toast.LENGTH_LONG).show()
            false
        } else {
            if (Patterns.EMAIL_ADDRESS.matcher(text1).matches()) {
                Toast.makeText(requireActivity(), VALID_EMAIL_TOAST, Toast.LENGTH_LONG).show()
                true
            } else {
                Toast.makeText(requireActivity(), INVALID_EMAIL_TOAST, Toast.LENGTH_LONG).show()
                false
            }
        }
    }

    private fun navigateToHomeFragment() {
        val homeFragment = HomeFragment.newInstance(activeDiary = true)

        val fragmentManager = requireActivity().supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, homeFragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    companion object {
        fun newInstance() = FinishFragment()
        private const val VALID_EMAIL_TOAST = "המייל ישלח בזמן אקראי בעתיד הקרוב"
        private const val INVALID_EMAIL_TOAST = "כתובת מייל לא תקינה"
    }
}