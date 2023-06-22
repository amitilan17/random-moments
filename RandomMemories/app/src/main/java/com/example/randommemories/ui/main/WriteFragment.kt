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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.*
import java.util.*


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

    private fun doPhotoPrint() {
//        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.leaves_first_frame)
//        PrinterHelper.printBitmap(requireContext(), bitmap, "jobName");


        // this implementation works but opens the activity
//        activity?.also { context ->
//            PrintHelper(context).apply {
//                scaleMode = PrintHelper.SCALE_MODE_FIT
//            }.also { printHelper ->
//                val bitmap = BitmapFactory.decodeResource(resources, R.drawable.leaves_first_frame)
//                printHelper.printBitmap("droids.jpg - test print", bitmap)
//            }
//        }

//        val printManager = context?.getSystemService(Context.PRINT_SERVICE) as PrintManager
//        val jobName = "${getString(R.string.app_name)} Document"
//
//        printManager.print(
//            jobName,
//            PrintDocumentAdapter(),
//            PrintAttributes.Builder()
//                .setMediaSize(PrintAttributes.MediaSize.ISO_A4.asLandscape())
//                .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
//                .setResolution(PrintAttributes.Resolution("best", "best", 300, 300))
//                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
//                .build()
//        )

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

        val printButton = view?.findViewById<Button>(R.id.print_button)
        printButton?.setOnClickListener {
            doPhotoPrint()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun takeImage() {
        lifecycleScope.launchWhenStarted {
            getTmpFileUri().let { uri -> takeImageResult.launch(uri) }
        }
    }


    private fun runPythonManipulation(imageUri: String?) { // todo handle nullable
        module.callAttr("manipulate_image", imageUri)
            ?.toJava(ByteArray::class.java)
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
//        runPythonManipulation(imageUri.path)
        val bitmap = BitmapFactory.decodeStream(
            requireContext().contentResolver.openInputStream(imageUri) // todo take the python output file
        )

//        createCanvasAndSaveToFile(userTypedText, bitmap)
//        deleteFile(imageUri)
        navigateToFinishFragment()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createCanvasAndSaveToFile(text: String?, image: Bitmap) {
        try {
            // create full layout
            val font: Typeface? =
                ResourcesCompat.getFont(requireContext(), R.font.parmigiano_sans_light)
            val bitmap = vm.createLayout(text, image, font!!)

            // Save the bitmap as a JPEG file
            val outputFile = File(requireActivity().filesDir, "layout.jpg")
            val outputStream = FileOutputStream(outputFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
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

    private fun showSnoozeDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.snooze_dialog, null)
        dialogView.findViewById<Button>(R.id.accept_snooze_button).setOnClickListener {
            // TODO - move to main frag
        }


        val builder = AlertDialog.Builder(requireContext(), R.style.squareDialog)
            .setView(dialogView)

        val dialog = builder.create()

        dialogView.findViewById<Button>(R.id.back_snooze_button).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun showMoveToCameraDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.move_to_camera_dialog, null)
        dialogView.findViewById<Button>(R.id.accept_move_to_camera_button).setOnClickListener {
            userTypedText = editText?.text.toString()
            takeImage()
        }


        val builder = AlertDialog.Builder(requireContext(), R.style.squareDialog)
            .setView(dialogView)

        val dialog = builder.create()

        dialogView.findViewById<Button>(R.id.back_move_to_camera_button).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // TODO delete all pythons temp files
    private fun deleteFiles(uri: Uri) {
        val contentResolver: ContentResolver = requireActivity().contentResolver
        contentResolver.delete(uri, null, null)
    }

    // old- todo delete when sure that no coroutine needed and we can use runPythonManipulation2
    private fun runPythonManipulationOld(image: Bitmap) {
        runBlocking {
            launch(Dispatchers.IO) {
                try {
                    val imageBytearray = encodeBitmapToByteArray(image)

                    val bytes = module.callAttr("manipulate_image", imageBytearray)
                        .toJava(ByteArray::class.java)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)


                    val outputFile = File(requireActivity().filesDir, "output01.jpg")
                    val outputStream = FileOutputStream(outputFile)
                    bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    outputStream.close()
                } catch (e: PyException) {
                    Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun encodeBitmapToByteArray(bitmap: Bitmap): ByteArray {
        // Create an output stream for the compressed bitmap data
        val outputStream = ByteArrayOutputStream()

        // Compress the bitmap into the output stream as a JPEG with 100% quality
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)

        // Convert the output stream to a byte array and return
        return outputStream.toByteArray()
    }

    companion object {
        fun newInstance() = WriteFragment()
        private const val MAX_CHARACTERS = 1200
    }
}