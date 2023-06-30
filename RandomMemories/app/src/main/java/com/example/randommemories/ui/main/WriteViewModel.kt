package com.example.randommemories.ui.main

import android.graphics.*
import android.graphics.text.LineBreaker
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel

class WriteViewModel() : ViewModel() {
    val randomStringsFemale = arrayOf(
        "עכשיו אני מרגישה",
        "מה שאני רואה מולי עכשיו זה",
        "עכשיו אני חושבת על",
        "מה שמעסיק אותי כרגע זה",
        "אני נמצאת ב"
    )
    val randomStringsMale = arrayOf(
        "עכשיו אני מרגיש",
        "מה שאני רואה מולי עכשיו זה",
        "עכשיו אני חושב על",
        "מה שמעסיק אותי כרגע זה",
        "אני נמצא ב"
    )

    var userTypedText: String? = null

//    private val randomString: String
//        get() = randomStrings.random()

//    val currentText: String
//        get() = userTypedText ?: randomString

    @RequiresApi(Build.VERSION_CODES.Q)
    fun createLayout(text: String?, manipulatedImage: Bitmap, font: Typeface): Bitmap {
        val outputBitmap = Bitmap.createBitmap(OUTPUT_WIDTH, OUTPUT_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)

        // Draw a white rectangle over the entire canvas
        val backgroundPaint = Paint()
        backgroundPaint.color = Color.WHITE
        canvas.drawRect(
            0F, 0F, canvas.width.toFloat(),
            canvas.height.toFloat(), backgroundPaint
        )

        // Create a Paint object for drawing text
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize =
                (FONT_SIZE * 300) / 72 // FONT_SIZE * resources.displayMetrics.density todo try
            color = Color.BLACK
            typeface = font
        }

        // Define the bounding box for the text
        val boundingBox = Rect(TEXT_BOX_LEFT, TEXT_BOX_TOP, TEXT_BOX_RIGHT, TEXT_BOX_BOTTOM)

        // Create a StaticLayout object to split the text into multiple lines
        val staticLayout =
            text?.let {
                StaticLayout.Builder.obtain(it, 0, text.length, textPaint, boundingBox.width())
                    .setLineSpacing(0f, 1f)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setIncludePad(true)
                    .setBreakStrategy(LineBreaker.BREAK_STRATEGY_HIGH_QUALITY)
                    .build()
            }

        // Draw the text on the canvas
        canvas.save()
        canvas.translate(boundingBox.left.toFloat(), boundingBox.top.toFloat())
        staticLayout?.draw(canvas)
        canvas.restore()

        // Draw the image on the canvas at the position (100, 300)
        val scaleFactor = USER_IMAGE_WIDTH.toFloat() / manipulatedImage.width
        val newHeight = (manipulatedImage.height * scaleFactor).toInt()
        val resizedImage =
            Bitmap.createScaledBitmap(manipulatedImage, USER_IMAGE_WIDTH, newHeight, false)
        val matrix = Matrix()
        matrix.postRotate(90f) // todo always?
        val rotatedImage = Bitmap.createBitmap(
            resizedImage, 0, 0,
            resizedImage.width, resizedImage.height, matrix, true
        )
        canvas.drawBitmap(rotatedImage, USER_IMAGE_LEFT_POSITION, USER_IMAGE_TOP_POSITION, null)

        return outputBitmap
    }

    // todo remove
    private fun manipulateImage(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val appliedShader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            Color.WHITE, Color.parseColor("#6b6bd4"),
            Shader.TileMode.CLAMP
        )

        val paint = Paint().apply {
            shader = appliedShader
            xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
        }

        Canvas(outputBitmap).apply {
            drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            drawBitmap(bitmap, 0f, 0f, null)
        }

        return outputBitmap
    }

    companion object {
        private const val OUTPUT_HEIGHT = 1653
        private const val OUTPUT_WIDTH = 1181 * 2
        private const val TEXT_BOX_TOP = 780
        private const val TEXT_BOX_LEFT = 100
        private const val TEXT_BOX_RIGHT = 968
        private const val TEXT_BOX_BOTTOM = 1500
        private const val USER_IMAGE_WIDTH = 886
        private const val USER_IMAGE_TOP_POSITION = 307F
        private const val USER_IMAGE_LEFT_POSITION = 1335F
        private const val FONT_SIZE = 9F
    }
}