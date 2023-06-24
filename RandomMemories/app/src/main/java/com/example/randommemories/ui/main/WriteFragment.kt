package com.example.randommemories.ui.main


import TakePictureWithUriReturnContract
import android.annotation.SuppressLint
import android.content.ContentResolver
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
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.chaquo.python.PyException
import com.chaquo.python.Python
import com.example.randommemories.*
import com.example.randommemories.databinding.FragmentWriteBinding
import com.example.randommemories.helpers.LocaleHelper
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.*
import java.util.*
import java.io.File


class WriteFragment : Fragment() {
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

    private lateinit var binding: FragmentWriteBinding
    private val vm: WriteViewModel by viewModels()


    // todo move
    private val py = Python.getInstance()
    private val module = py.getModule("manipulate_image")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleHelper.onCreate(requireContext(), "he")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWriteBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.vm = vm

        editText = binding.root.findViewById(R.id.edit_text)
        counterTextView = binding.root.findViewById(R.id.char_counter)
        editText?.addTextChangedListener(textWatcher)
        editText?.filters = arrayOf<InputFilter>(LengthFilter(MAX_CHARACTERS))

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("QueryPermissionsNeeded")
    private fun setupViews() {
        val editText = view?.findViewById<EditText>(R.id.edit_text)
        val continueButton = view?.findViewById<Button>(R.id.continue_button)
        val snoozeButton = view?.findViewById<Button>(R.id.snooze_button)

        continueButton?.setOnClickListener {
            showMoveToCameraDialog()
        }

        snoozeButton?.setOnClickListener {
            showSnoozeDialog()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun takeImage() {
        lifecycleScope.launchWhenStarted {
            getTmpFileUri().let { uri -> takeImageResult.launch(uri) }
        }
    }


    private fun getTmpFileUri(): Uri {
        val tmpFile =
            File.createTempFile("tmp_image_file", ".png", requireContext().cacheDir).apply {
                createNewFile()
                deleteOnExit()
            }

        return FileProvider.getUriForFile(
            requireContext(),
            "${BuildConfig.APPLICATION_ID}.provider",
            tmpFile
        )
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun onImageTaken(imageUri: Uri) {
//        deleteFile(imageUri)
        navigateToFinishFragment()
        sendToPrintServer(imageUri.getFile())
    }

    private fun Uri.getFile(): File? {
        val inputStream = requireContext().contentResolver.openInputStream(this)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val file = this.lastPathSegment?.let { File(requireContext().cacheDir, it) }
        val outStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
        outStream.flush()
        outStream.close()
        return file
    }

    private fun sendToPrintServer(image: File?) {
        if (image == null) {
            return
        }
        val client = OkHttpClient()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("text", userTypedText ?: "")
            .addFormDataPart(
                "image", "image.jpg",
                RequestBody.create("image/png".toMediaTypeOrNull(), image)
            )
            .build()

        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) {
                async {
                    val request = Request.Builder()
                        .url("http://10.0.0.2:8000/data")
                        .post(requestBody)
                        .build()

                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    response.body?.string()
                }
            }
            println(result.await())
        }
    }

    private fun navigateToFinishFragment() {
        val finishFragment = FinishFragment()

        val fragmentManager = requireActivity().supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, finishFragment)
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
        val builder = AlertDialog.Builder(requireContext(), R.style.squareDialog)
            .setView(dialogView)

        val dialog = builder.create()
        dialogView.findViewById<Button>(R.id.back_snooze_button).setOnClickListener {
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
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.move_to_camera_dialog, null)
        val builder = AlertDialog.Builder(requireContext(), R.style.squareDialog)
            .setView(dialogView)

        val dialog = builder.create()

        dialogView.findViewById<Button>(R.id.back_move_to_camera_button).setOnClickListener {
            dialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.accept_move_to_camera_button).setOnClickListener {
            userTypedText = editText?.text.toString()
            takeImage()
            dialog.dismiss()
        }

        dialog.show()
    }

    // TODO delete all pythons temp files
    private fun deleteFiles(uri: Uri) {
        val contentResolver: ContentResolver = requireActivity().contentResolver
        contentResolver.delete(uri, null, null)
    }

    companion object {
        private const val MAX_CHARACTERS = 1200
    }
}