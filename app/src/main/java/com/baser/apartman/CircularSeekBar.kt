package com.baser.apartman.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class CircularSeekBar @JvmOverloads constructor(
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
        color = Color.parseColor("#FF9800")
    }

    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val thumbStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 16f
        color = Color.parseColor("#FF9800")
    }

    private var minValue = 10f
    private var maxValue = 30f
    private var currentValue = 21f
    private var strokeWidth = 18f
    private var startAngle = 120f
    private var sweepAngle = 300f
    private var thumbRadius = 12f

    private var onChangeListener: ((Float) -> Unit)? = null

    init {
        // R KULLANMADAN - XML attributelerini manuel oku
        attrs?.let { attributeSet ->
            for (i in 0 until attributeSet.attributeCount) {
                when (attributeSet.getAttributeName(i)) {
                    "strokeWidth" -> {
                        val value = attributeSet.getAttributeValue(i)
                        strokeWidth = parseDimension(value, 12f)
                    }
                    "minValue" -> {
                        val value = attributeSet.getAttributeValue(i)
                        minValue = value.toFloatOrNull() ?: 10f
                    }
                    "maxValue" -> {
                        val value = attributeSet.getAttributeValue(i)
                        maxValue = value.toFloatOrNull() ?: 30f
                    }
                    "startAngle" -> {
                        val value = attributeSet.getAttributeValue(i)
                        startAngle = value.toFloatOrNull() ?: 120f
                    }
                    "sweepAngle" -> {
                        val value = attributeSet.getAttributeValue(i)
                        sweepAngle = value.toFloatOrNull() ?: 300f
                    }
                    "progressColor" -> {
                        val value = attributeSet.getAttributeValue(i)
                        if (value.startsWith("@color/")) {
                            // Varsayılan renk
                            progressPaint.color = Color.parseColor("#FF9800")
                            thumbStrokePaint.color = Color.parseColor("#FF9800")
                        } else if (value.startsWith("#")) {
                            try {
                                progressPaint.color = Color.parseColor(value)
                                thumbStrokePaint.color = Color.parseColor(value)
                            } catch (e: Exception) {
                                progressPaint.color = Color.parseColor("#FF9800")
                                thumbStrokePaint.color = Color.parseColor("#FF9800")
                            }
                        }
                    }
                    "backgroundStrokeColor" -> {
                        val value = attributeSet.getAttributeValue(i)
                        if (value.startsWith("@color/")) {
                            // Varsayılan renk
                            backgroundPaint.color = Color.parseColor("#E0E0E0")
                        } else if (value.startsWith("#")) {
                            try {
                                backgroundPaint.color = Color.parseColor(value)
                            } catch (e: Exception) {
                                backgroundPaint.color = Color.parseColor("#E0E0E0")
                            }
                        }
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

    fun setValue(value: Float) {
        currentValue = value.coerceIn(minValue, maxValue)
        invalidate()
    }

    fun getValue(): Float = currentValue

    fun setOnChangeListener(listener: (Float) -> Unit) {
        onChangeListener = listener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (min(width, height) / 2f) - (strokeWidth * 2)

        // Arkaplan çemberi
        val rect = RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )
        canvas.drawArc(rect, startAngle, sweepAngle, false, backgroundPaint)

        // İlerleme çemberi
        val progressPercent = (currentValue - minValue) / (maxValue - minValue)
        val progressSweep = sweepAngle * progressPercent
        canvas.drawArc(rect, startAngle, progressSweep, false, progressPaint)

        // Thumb (kaydırıcı) pozisyonu
        val thumbAngle = startAngle + progressSweep
        val thumbRadians = Math.toRadians(thumbAngle.toDouble())
        val thumbX = centerX + (radius * cos(thumbRadians)).toFloat()
        val thumbY = centerY + (radius * sin(thumbRadians)).toFloat()

        // Thumb çiz
        canvas.drawCircle(thumbX, thumbY, thumbRadius, thumbPaint)
        canvas.drawCircle(thumbX, thumbY, thumbRadius, thumbStrokePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                updateValueFromTouch(event.x, event.y)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateValueFromTouch(x: Float, y: Float) {
        val centerX = width / 2f
        val centerY = height / 2f

        var angle = Math.toDegrees(atan2((y - centerY).toDouble(), (x - centerX).toDouble())).toFloat()
        if (angle < 0) angle += 360f

        var normalizedAngle = angle - startAngle
        if (normalizedAngle < 0) normalizedAngle += 360f

        if (normalizedAngle <= sweepAngle) {
            val progressPercent = normalizedAngle / sweepAngle
            currentValue = minValue + (maxValue - minValue) * progressPercent
            onChangeListener?.invoke(currentValue)
            invalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredSize = 200
        val size = min(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        ).coerceAtLeast(desiredSize)

        setMeasuredDimension(size, size)
    }
}