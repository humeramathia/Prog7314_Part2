package com.example.prog7314_part2.ui.calendar

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.prog7314_part2.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Month grid that can draw teal markers on days that have fixtures.
 * Replaces the stock [android.widget.CalendarView], which cannot decorate
 * individual days.
 */
class EventMonthView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    var onDaySelected: ((year: Int, month: Int, day: Int) -> Unit)? = null
    var onMonthChanged: (() -> Unit)? = null

    private val visibleMonth: Calendar = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    private var selectedDay: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    private var markedDays: Set<Int> = emptySet()
    private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    init {
        orientation = VERTICAL
        rebuild()
    }

    fun setEventTimestamps(timestamps: List<Long>) {
        val probe = Calendar.getInstance()
        markedDays = timestamps.mapNotNull { millis ->
            probe.timeInMillis = millis
            val sameMonth =
                probe.get(Calendar.YEAR) == visibleMonth.get(Calendar.YEAR) &&
                    probe.get(Calendar.MONTH) == visibleMonth.get(Calendar.MONTH)
            if (sameMonth) probe.get(Calendar.DAY_OF_MONTH) else null
        }.toSet()
        rebuild()
    }

    private fun rebuild() {
        removeAllViews()
        addView(monthHeader())
        addView(weekdayRow())
        addView(dayGrid())
    }

    private fun monthHeader(): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        val prev = headerButton("‹") {
            visibleMonth.add(Calendar.MONTH, -1)
            selectedDay = 1
            onMonthChanged?.invoke()
            fireSelection()
            rebuild()
        }
        val next = headerButton("›") {
            visibleMonth.add(Calendar.MONTH, 1)
            selectedDay = 1
            onMonthChanged?.invoke()
            fireSelection()
            rebuild()
        }
        val label = TextView(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            gravity = Gravity.CENTER
            text = monthFormat.format(visibleMonth.time)
            setTextColor(ContextCompat.getColor(context, R.color.slate))
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
        }
        row.addView(prev)
        row.addView(label)
        row.addView(next)
        return row
    }

    private fun headerButton(label: String, onClick: () -> Unit): TextView {
        return TextView(context).apply {
            text = label
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(8), dp(16), dp(8))
            setTextColor(ContextCompat.getColor(context, R.color.teal))
            setOnClickListener { onClick() }
        }
    }

    private fun weekdayRow(): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(0, dp(4), 0, dp(4))
        }
        val names = weekdayNames()
        names.forEach { name ->
            row.addView(TextView(context).apply {
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
                gravity = Gravity.CENTER
                text = name
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.muted))
            })
        }
        return row
    }

    private fun dayGrid(): GridLayout {
        val grid = GridLayout(context).apply {
            columnCount = 7
            rowCount = 6
        }
        val first = (visibleMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val firstWeekday = first.get(Calendar.DAY_OF_WEEK)
        val startOffset = (firstWeekday - first.firstDayOfWeek + 7) % 7
        val daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH)
        repeat(42) { index ->
            val dayNumber = index - startOffset + 1
            val cell = TextView(context).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = dp(44)
                    columnSpec = GridLayout.spec(index % 7, 1f)
                    rowSpec = GridLayout.spec(index / 7)
                }
                gravity = Gravity.CENTER
                textSize = 14f
                if (dayNumber in 1..daysInMonth) {
                    val marked = dayNumber in markedDays
                    text = if (marked) "$dayNumber\n●" else dayNumber.toString()
                    setTextColor(
                        ContextCompat.getColor(
                            context,
                            when {
                                dayNumber == selectedDay -> R.color.black
                                marked -> R.color.teal
                                else -> R.color.slate
                            }
                        )
                    )
                    if (dayNumber == selectedDay) {
                        setBackgroundResource(R.drawable.bg_day_selected)
                    } else if (marked) {
                        setBackgroundResource(R.drawable.bg_day_marked)
                    } else {
                        background = null
                    }
                    setOnClickListener {
                        selectedDay = dayNumber
                        fireSelection()
                        rebuild()
                    }
                } else {
                    text = ""
                }
            }
            grid.addView(cell)
        }
        return grid
    }

    private fun fireSelection() {
        onDaySelected?.invoke(
            visibleMonth.get(Calendar.YEAR),
            visibleMonth.get(Calendar.MONTH),
            selectedDay
        )
    }

    private fun weekdayNames(): List<String> {
        val format = SimpleDateFormat("EEE", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        return List(7) {
            val label = format.format(cal.time).take(2)
            cal.add(Calendar.DAY_OF_MONTH, 1)
            label
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
