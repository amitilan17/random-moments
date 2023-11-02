package com.example.randommemories.ui.main

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.randommemories.LaunchActivity
import com.example.randommemories.R
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit


/**
 * A simple [Fragment] subclass.
 * Use the [FinishFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FinishFragment(private val userText: String?, private val userImageUri: Uri) : Fragment() {

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
//            navigateToHomeFragment() // todo switch with dismiss for smoother animation?
            deleteFiles(userImageUri)
            dialog.dismiss()

            val intent = Intent(requireContext(), LaunchActivity::class.java)
            startActivity(intent)
        }
        dialogView.findViewById<Button>(R.id.send_button).setOnClickListener {
            if (validateEmail(emailEditText.text)) {
                sendToEmailServer(emailEditText.text.toString())
//                navigateToHomeFragment() // todo switch with dismiss for smoother animation?
                dialog.dismiss()
                val intent = Intent(requireContext(), LaunchActivity::class.java)
                startActivity(intent)
            }
        }
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()

    }


    private fun sendToEmailServer(email: String) {
        val image = userImageUri.getFile() ?: return
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("text", userText ?: "")
            .addFormDataPart(
                "image", "image.jpg",
                RequestBody.create("image/png".toMediaTypeOrNull(), image)
            )
            .addFormDataPart("email", email)
            .build()

        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) {
                async {
                    try {
                        val request = Request.Builder()
//                            .url("http://10.0.0.2:8000/data")
                            .url("https://serene-springs-36453.herokuapp.com/data")
//                            .url("http://192.168.217.180:8000/data")
                            .post(requestBody)
                            .build()

                        val response = client.newCall(request).execute()
                        if (!response.isSuccessful) throw IOException("Unexpected code $response")
                        response.body?.string()
                    } catch (e: Exception) {
                        println("email server error: $e")
                    }
                    deleteFiles(userImageUri)
                }
            }
            println(result.await())
        }
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


    private fun deleteFiles(uri: Uri) {
        val contentResolver: ContentResolver = requireActivity().contentResolver
        contentResolver.delete(uri, null, null)
    }


    @SuppressLint("ResourceAsColor")
    private fun validateEmail(text1: Editable): Boolean {
        return if (text1.isEmpty()) {
            Toast.makeText(requireActivity(), INVALID_EMAIL_TOAST, Toast.LENGTH_LONG).show()
            false
        } else {
            if (Patterns.EMAIL_ADDRESS.matcher(text1).matches()) {
                Toast.makeText(requireActivity(), VALID_EMAIL_TOAST, Toast.LENGTH_LONG).show()
                true
            } else {
                val toast = Toast.makeText(requireActivity(), INVALID_EMAIL_TOAST, Toast.LENGTH_LONG)
                toast.show()
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
        fun newInstance(userText: String?, userImageUri: Uri) = FinishFragment(userText, userImageUri)
        private const val VALID_EMAIL_TOAST = "המייל ישלח בזמן אקראי בעתיד הקרוב"
        private const val INVALID_EMAIL_TOAST = "כתובת מייל לא תקינה"
    }
}