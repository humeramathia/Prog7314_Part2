package com.example.prog7314_part2.ui.performance

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.prog7314_part2.R

class SimpleBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.teal)
        style = Paint.Style.FILL
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.muted)
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }
    private var points: List<Pair<String, Float>> = emptyList()

    fun setData(values: List<Pair<String, Float>>) {
        points = values
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.isEmpty()) return
        val max = points.maxOf { it.second }.coerceAtLeast(1f)
        val barWidth = width / (points.size * 1.6f)
        val chartHeight = height - 48f
        points.forEachIndexed { index, (label, value) ->
            val left = 24f + index * (barWidth * 1.6f)
            val barHeight = (value / max) * (chartHeight - 16f)
            val top = chartHeight - barHeight
            canvas.drawRoundRect(RectF(left, top, left + barWidth, chartHeight), 12f, 12f, barPaint)
            canvas.drawText(label, left + barWidth / 2f, height - 8f, labelPaint)
        }
    }
}
