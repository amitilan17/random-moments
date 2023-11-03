package com.example.randommemories.mainFlow

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
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class MomentSavedFragment(private val userText: String?, private val userImageUri: Uri) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_moment_saved, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            // block back button
        }

        val backButton = view.findViewById<Button>(R.id.finish_exit_button)
        backButton.setOnClickListener {
            showSendToEmailDialog()
        }
    }

    private fun showSendToEmailDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.send_to_email_dialog, null)
        val dialog = AlertDialog.Builder(requireContext(), R.style.squareDialog).setView(dialogView).create()

        val emailEditText = dialogView.findViewById<EditText>(R.id.email_editText)

        dialogView.findViewById<Button>(R.id.exit_button).setOnClickListener {
            deleteFiles(userImageUri)
            dialog.dismiss()
            val intent = Intent(requireContext(), LaunchActivity::class.java)
            startActivity(intent)
        }

        dialogView.findViewById<Button>(R.id.send_button).setOnClickListener {
            if (validateEmail(emailEditText.text)) {
                sendToEmailServer(emailEditText.text.toString())
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
                image.asRequestBody("image/png".toMediaTypeOrNull())
            )
            .addFormDataPart("email", email)
            .build()

        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) {
                async {
                    try {
                        val request = Request.Builder()
                            .url(HEROKU_URL)
                            .post(requestBody)
                            .build()
                        val response = client.newCall(request).execute()
                        if (!response.isSuccessful) {
                            throw IOException("Client call failed with response: $response")
                        }
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

    companion object {
        private const val VALID_EMAIL_TOAST = "המייל ישלח בזמן אקראי בעתיד הקרוב"
        private const val INVALID_EMAIL_TOAST = "כתובת מייל לא תקינה"
        private const val HEROKU_URL = "https://serene-springs-36453.herokuapp.com/data"
        private const val LOCAL_IP_URL = "http://10.0.0.2:8000/data"
    }
}