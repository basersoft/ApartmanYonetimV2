package com.baser.apartman.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import kotlin.math.min

class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#E0E0E0")
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#FF5722")
    }

    private var progress = 22f
    private var maxProgress = 40f
    private var strokeWidth = 12f

    init {
        // XML attributelerini manuel oku
        attrs?.let { attributeSet ->
            for (i in 0 until attributeSet.attributeCount) {
                when (attributeSet.getAttributeName(i)) {
                    "strokeWidth" -> {
                        val value = attributeSet.getAttributeValue(i)
                        strokeWidth = parseDimension(value, 12f)
                    }
                    "maxProgress" -> {
                        val value = attributeSet.getAttributeValue(i)
                        maxProgress = value.toFloatOrNull() ?: 40f
                    }
                    "progressColor" -> {
                        val value = attributeSet.getAttributeValue(i)
                        if (value.startsWith("@color/")) {
                            // Varsayılan renk
                            progressPaint.color = Color.parseColor("#FF5722")
                        } else if (value.startsWith("#")) {
                            try {
                                progressPaint.color = Color.parseColor(value)
                            } catch (e: Exception) {
                                progressPaint.color = Color.parseColor("#FF5722")
                            }
                        }
                    }
                    "showLabel" -> {
                        // Bu attribute artık kullanılmıyor, ignore et
                    }
                }
            }
        }

        backgroundPaint.strokeWidth = strokeWidth
        progressPaint.strokeWidth = strokeWidth
    }

    private fun parseDimension(value: String, default: Float): Float {
        return try {
            if (value.endsWith("dp") || value.endsWith("dip")) {
                val numericValue = value.replace("dp", "").replace("dip", "").toFloat()
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    numericValue,
                    resources.displayMetrics
                )
            } else if (value.endsWith("sp")) {
                val numericValue = value.replace("sp", "").toFloat()
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP,
                    numericValue,
                    resources.displayMetrics
                )
            } else if (value.endsWith("px")) {
                value.replace("px", "").toFloat()
            } else {
                value.toFloat()
            }
        } catch (e: Exception) {
            default
        }
    }

    fun setProgress(value: Float) {
        progress = value.coerceIn(0f, maxProgress)
        invalidate()
    }

    fun setProgressColor(color: Int) {
        progressPaint.color = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (min(width, height) / 2f) - (strokeWidth * 2)

        // Arkaplan çemberi
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)

        // İlerleme çemberi
        val sweepAngle = 360f * (progress / maxProgress)
        val rect = RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )
        canvas.drawArc(rect, -90f, sweepAngle, false, progressPaint)

        // TEXT ÇİZİMİ YOK! Sadece progress bar
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredSize = 130 // dp - layout'taki değerle aynı (130dp)
        val size = min(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        ).coerceAtLeast(desiredSize)

        setMeasuredDimension(size, size)
    }
}