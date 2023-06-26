package com.example.randommemories.ui.main

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import androidx.annotation.RequiresApi
import com.example.randommemories.R


@RequiresApi(Build.VERSION_CODES.M)
class LinedEditText(context: Context, attrs: AttributeSet?) : androidx.appcompat.widget.AppCompatEditText(context, attrs) {
    private val mRect: Rect = Rect()
    private val mPaint: Paint = Paint()
//    private var isFirstClick = true

    // we need this constructor for LayoutInflater
    init {
        mPaint.style = Paint.Style.FILL_AND_STROKE
        mPaint.color = context.getColor(R.color.light_blue)
        mPaint.strokeWidth = 3f
        mPaint.pathEffect = DashPathEffect(floatArrayOf(3f, 15f), 0f) // Set dash pattern
    }

    override fun onDraw(canvas: Canvas) {
        val height = height
        val line_height = lineHeight
        var count = height / line_height
        if (lineCount > count) count = lineCount //for long text with scrolling
        val r: Rect = mRect
        val paint: Paint = mPaint
        var baseline = getLineBounds(0, r)+6f //first line
        for (i in 0 until count) {
            canvas.drawLine(r.left.toFloat(), (baseline + 1),
                r.right.toFloat(), (baseline + 1), paint)
            baseline += (lineHeight+0.2f) //next line
        }
        super.onDraw(canvas)
    }

//    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
//        super.onSelectionChanged(selStart, selEnd)
//        if (isFirstClick) {
//            isFirstClick = false
//            // Move cursor to the end of the text
//            setSelection(text?.length ?: 0)
//        }
//    }
}