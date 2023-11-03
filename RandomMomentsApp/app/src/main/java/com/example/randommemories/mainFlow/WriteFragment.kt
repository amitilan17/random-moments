package com.example.randommemories.mainFlow


import TakePictureWithUriReturnContract
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.print.*
import android.text.Editable
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.randommemories.*
import com.example.randommemories.databinding.FragmentWriteBinding
import com.example.randommemories.helpers.LocaleHelper
import kotlinx.coroutines.*
import okhttp3.*
import java.io.*
import java.util.*


class WriteFragment : Fragment() {
    private lateinit var binding: FragmentWriteBinding
    private val vm: WriteViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var userTypedText: String? = null
    private var editText: EditText? = null
    private var counterTextView: TextView? = null
    private val textWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            // Not used in this implementation
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            vm.userTypedText = s.toString()
        }

        override fun afterTextChanged(s: Editable) {
            val currentLength = s.length
            counterTextView?.text = getString(
                R.string.character_counter,
                currentLength,
                MAX_CHARACTERS
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private val takeImageResult =
        registerForActivityResult(TakePictureWithUriReturnContract()) { (isSuccess, imageUri) ->
            if (isSuccess) {
                onImageTaken(imageUri)
            }
        }


    override fun onResume() {
        super.onResume()
        LocaleHelper.onCreate(requireContext(), "he")
        activity?.findViewById<Button>(R.id.menu_button)?.visibility = View.INVISIBLE

    }

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWriteBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.vm = vm

        val isGenderFemale = sharedViewModel.genderIsFemale
        binding.currentText = if (isGenderFemale) {
            vm.randomStringsFemale.random()
        } else {
            vm.randomStringsMale.random()
        }

        editText = binding.root.findViewById(R.id.edit_text)
        counterTextView = binding.root.findViewById(R.id.char_counter)
        editText?.addTextChangedListener(textWatcher)
        editText?.filters = arrayOf<InputFilter>(LengthFilter(MAX_CHARACTERS))

        binding.root.setOnApplyWindowInsetsListener { _, _ ->
            applyKeyboardInsets(binding.root)
        }

        return binding.root
    }


    @RequiresApi(Build.VERSION_CODES.R)
    private fun applyKeyboardInsets(root: View): WindowInsets {

        val insets = root.rootWindowInsets.getInsets(WindowInsets.Type.ime())
        root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            updateMargins(insets.left, insets.top, insets.right, insets.bottom)
        }
        return WindowInsets.Builder()
            .setInsets(WindowInsets.Type.ime(), insets)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("QueryPermissionsNeeded")
    private fun setupViews() {
        val continueButton = view?.findViewById<Button>(R.id.continue_button)
        val snoozeButton = view?.findViewById<Button>(R.id.snooze_button)

        continueButton?.setOnClickListener {
            val imm =
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            imm!!.hideSoftInputFromWindow(requireView().windowToken, 0)
            showMoveToCameraDialog()
        }

        snoozeButton?.setOnClickListener {
            val imm =
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            imm!!.hideSoftInputFromWindow(requireView().windowToken, 0)
            showSnoozeDialog()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun takeImage() {
        lifecycleScope.launch {
            getTakenImageDestUri().let { destUri -> takeImageResult.launch(destUri) }
        }
    }

    private fun getTakenImageDestUri(): Uri {
        val tempFile =
            File.createTempFile("temp_image_file", ".png", requireContext().cacheDir).apply {
                createNewFile()
                deleteOnExit()
            }

        return FileProvider.getUriForFile(
            requireContext(),
            "${BuildConfig.APPLICATION_ID}.provider",
            tempFile
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun onImageTaken(imageUri: Uri) {
        navigateToMomentSavedFragment(imageUri)
    }

    private fun navigateToMomentSavedFragment(imageTakenUri: Uri) {
        val momentSavedFragment = MomentSavedFragment(userTypedText, imageTakenUri)
        val fragmentManager = requireActivity().supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, momentSavedFragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun navigateToHomeFragment() {
        val homeFragment = HomeFragment.newInstance(true)
        val fragmentManager = requireActivity().supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, homeFragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun showSnoozeDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.snooze_dialog, null)
        val dialog = AlertDialog.Builder(requireContext(), R.style.squareDialog).setView(dialogView).create()

        dialogView.findViewById<Button>(R.id.reject_snooze_button).setOnClickListener {
            dialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.accept_snooze_button).setOnClickListener {
            navigateToHomeFragment()
            dialog.dismiss()
        }

        dialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun showMoveToCameraDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.move_to_camera_dialog, null)
        val dialog = AlertDialog.Builder(requireContext(), R.style.squareDialog).setView(dialogView).create()

        dialogView.findViewById<Button>(R.id.back_move_to_camera_button).setOnClickListener {
            dialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.accept_move_to_camera_button).setOnClickListener {
            dialog.dismiss()
            userTypedText = editText?.text.toString()
            takeImage()
        }

        dialog.show()
    }

    companion object {
        private const val MAX_CHARACTERS = 1200
    }
}