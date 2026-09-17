package com.example.prog7314_part2.ui.performance

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.prog7314_part2.R
import java.text.NumberFormat

class SimpleBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val density = resources.displayMetrics.density

    private fun dp(value: Float): Float = value * density

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.teal)
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.slate)
        textSize = dp(12f)
        textAlign = Paint.Align.CENTER
    }

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.muted)
        strokeWidth = dp(1f)
    }

    private val numberFormat = NumberFormat.getNumberInstance().apply {
        maximumFractionDigits = 2
    }

    private var points: List<Pair<String, Double>> = emptyList()
    private var valueLabels: List<String> = emptyList()
    private var slotWidth = dp(64f)

    fun setData(values: List<Pair<String, Double>>) {
        points = values.filter {
            it.second.isFinite() && it.second >= 0.0
        }

        valueLabels = points.map { numberFormat.format(it.second) }

        val widestLabel = valueLabels.maxOfOrNull {
            labelPaint.measureText(it)
        } ?: 0f

        slotWidth = maxOf(dp(64f), widestLabel + dp(20f))

        requestLayout()
        invalidate()
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int
    ) {
        val desiredWidth = maxOf(
            dp(260f),
            dp(32f) + slotWidth * points.size
        ).toInt()

        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(dp(280f).toInt(), heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.isEmpty()) return

        val leftEdge = dp(16f)
        val rightEdge = width.toFloat() - dp(16f)
        val topEdge = dp(32f)
        val baseline = height.toFloat() - dp(52f)
        val plotHeight = baseline - topEdge

        if (plotHeight <= 0f || rightEdge <= leftEdge) return

        val maximum = points.maxOf { it.second }.coerceAtLeast(1.0)
        val slot = (rightEdge - leftEdge) / points.size
        val barWidth = minOf(dp(40f), slot * 0.6f)

        canvas.drawLine(
            leftEdge, baseline,
            rightEdge, baseline,
            axisPaint
        )

        points.forEachIndexed { index, (day, value) ->
            val centre = leftEdge + slot * (index + 0.5f)
            val barHeight = ((value / maximum) * plotHeight).toFloat()
            val top = baseline - barHeight

            if (value > 0.0) {
                canvas.drawRect(
                    centre - barWidth / 2f,
                    top,
                    centre + barWidth / 2f,
                    baseline,
                    barPaint
                )
            } else {
                // Zero is a recorded value, not a missing entry.
                canvas.drawCircle(centre, baseline, dp(3f), barPaint)
            }

            canvas.drawText(
                valueLabels[index],
                centre,
                top - dp(8f),
                labelPaint
            )

            canvas.drawText(
                day,
                centre,
                baseline + dp(20f),
                labelPaint
            )
        }

        canvas.drawText(
            "Day of month",
            width / 2f,
            height - dp(8f),
            labelPaint
        )
    }
}